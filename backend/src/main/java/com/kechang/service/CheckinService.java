package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.dto.CheckinCreateDTO;
import com.kechang.dto.CheckinSubmitDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 课堂签到服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinService {

    private final CheckinSessionMapper sessionMapper;
    private final CheckinRecordMapper recordMapper;
    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final SysUserMapper sysUserMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final ParticipationPointMapper pointMapper;

    // ==================== 教师端 ====================
    /**
     * 教师创建签到场次
     */
    @Transactional
    public Map<String, Object> create(CheckinCreateDTO dto, Long teacherId) {
        if (dto.getCourseId() == null) {
            throw BusinessException.badRequest("请选择课程");
        }
        Course course = courseMapper.selectById(dto.getCourseId());
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        if (!Objects.equals(course.getTeacherId(), teacherId)) {
            throw BusinessException.forbidden("只能在自己的课程中发起签到");
        }

        String type = StringUtils.hasText(dto.getCheckinType()) ? dto.getCheckinType() : "NUMERIC_CODE";
        if (!Set.of("NUMERIC_CODE", "LOCATION").contains(type)) {
            throw BusinessException.badRequest("不支持的签到方式: " + type);
        }

        // LOCATION方式校验位置参数
        if ("LOCATION".equals(type)) {
            if (dto.getLatitude() == null || dto.getLongitude() == null) {
                throw BusinessException.badRequest("地理位置签到需要设置经纬度");
            }
        }

        int durationMinutes = dto.getDurationMinutes() != null && dto.getDurationMinutes() > 0
                ? dto.getDurationMinutes() : 5;
        LocalDateTime now = LocalDateTime.now();

        CheckinSession session = new CheckinSession();
        session.setCourseId(dto.getCourseId());
        session.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : "课堂签到");
        session.setCheckinType(type);
        session.setCode(generateCode());
        session.setLatitude(dto.getLatitude());
        session.setLongitude(dto.getLongitude());
        session.setLocationRadius(dto.getLocationRadius() != null ? dto.getLocationRadius() : 100);
        session.setStartTime(now);
        session.setEndTime(now.plusMinutes(durationMinutes));
        session.setStatus("ACTIVE");
        session.setCreateBy(teacherId);
        sessionMapper.insert(session);

        return toSessionMap(session, course.getCourseName());
    }

    /**
     * 教师查看签到列表（分页）
     */
    public PageResult<Map<String, Object>> teacherList(Long teacherId, Long courseId, int page, int size) {
        LambdaQueryWrapper<CheckinSession> wrapper = new LambdaQueryWrapper<CheckinSession>()
                .eq(CheckinSession::getCreateBy, teacherId)
                .eq(courseId != null, CheckinSession::getCourseId, courseId)
                .orderByDesc(CheckinSession::getStartTime);
        Page<CheckinSession> mpPage = sessionMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> records = mpPage.getRecords().stream()
                .map(this::toSessionMapWithStats)
                .collect(Collectors.toList());
        return PageResult.of(records, mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize());
    }

    /**
     * 签到详情（教师看全量，学生看自己的记录）
     */
    public Map<String, Object> detail(Long sessionId, Long userId, String role) {
        CheckinSession session = getSession(sessionId);
        Course course = courseMapper.selectById(session.getCourseId());
        Map<String, Object> result = toSessionMap(session, course != null ? course.getCourseName() : null);

        if ("ROLE_TEACHER".equals(role) || Objects.equals(session.getCreateBy(), userId)) {
            // 教师：查全部签到记录
            List<CheckinRecord> records = recordMapper.selectList(
                    new LambdaQueryWrapper<CheckinRecord>()
                            .eq(CheckinRecord::getSessionId, sessionId)
                            .orderByAsc(CheckinRecord::getCheckinTime));
            result.put("records", records.stream().map(r -> toRecordMap(r, session)).collect(Collectors.toList()));
            result.put("stats", buildStats(session, records));
        } else {
            // 学生：仅看自己的记录
            CheckinRecord myRecord = recordMapper.selectOne(
                    new LambdaQueryWrapper<CheckinRecord>()
                            .eq(CheckinRecord::getSessionId, sessionId)
                            .eq(CheckinRecord::getStudentId, userId));
            result.put("myRecord", myRecord != null ? toRecordMap(myRecord, session) : null);
            // 隐藏签到码（仅教师可见）
            if (!Objects.equals(session.getCreateBy(), userId)) {
                result.remove("code");
            }
        }
        return result;
    }

    /**
     * 签到统计
     */
    public Map<String, Object> stats(Long sessionId, Long userId, String role) {
        CheckinSession session = getSession(sessionId);
        if (!"ROLE_TEACHER".equals(role) && !Objects.equals(session.getCreateBy(), userId)) {
            throw BusinessException.forbidden("仅教师可查看统计");
        }
        List<CheckinRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getSessionId, sessionId));
        return buildStats(session, records);
    }

    /**
     * 结束签到
     */
    @Transactional
    public void close(Long sessionId, Long teacherId) {
        CheckinSession session = getSession(sessionId);
        if (!Objects.equals(session.getCreateBy(), teacherId)) {
            throw BusinessException.forbidden("只能结束自己发起的签到");
        }
        if ("ENDED".equals(session.getStatus())) {
            throw BusinessException.badRequest("签到已结束");
        }
        endSession(session);
    }

    // ==================== 学生端 ====================
    /**
     * 学生查看当前可签到的活跃场次
     */
    public List<Map<String, Object>> studentActiveList(Long studentId) {
        // 获取学生所有选课
        List<Long> courseIds = courseStudentMapper.selectList(
                new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getStudentId, studentId))
                .stream().map(CourseStudent::getCourseId).collect(Collectors.toList());
        if (courseIds.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<CheckinSession> activeSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<CheckinSession>()
                        .in(CheckinSession::getCourseId, courseIds)
                        .eq(CheckinSession::getStatus, "ACTIVE")
                        .le(CheckinSession::getStartTime, now)
                        .gt(CheckinSession::getEndTime, now)
                        .orderByAsc(CheckinSession::getEndTime));

        return activeSessions.stream().map(session -> {
            Course course = courseMapper.selectById(session.getCourseId());
            Map<String, Object> map = toSessionMap(session, course != null ? course.getCourseName() : null);

            // 检查是否已签到
            CheckinRecord record = recordMapper.selectOne(
                    new LambdaQueryWrapper<CheckinRecord>()
                            .eq(CheckinRecord::getSessionId, session.getId())
                            .eq(CheckinRecord::getStudentId, studentId));
            map.put("checkedIn", record != null);
            map.put("myStatus", record != null ? record.getStatus() : null);

            // 判断迟到窗口
            long totalSeconds = ChronoUnit.SECONDS.between(session.getStartTime(), session.getEndTime());
            long lateThresholdSeconds = (long) (totalSeconds * 0.8);
            LocalDateTime lateTime = session.getStartTime().plusSeconds(lateThresholdSeconds);
            map.put("isLateWindow", now.isAfter(lateTime));
            map.put("lateTime", lateTime);

            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 学生执行签到
     */
    @Transactional
    public Map<String, Object> checkin(Long sessionId, CheckinSubmitDTO dto, Long studentId) {
        CheckinSession session = getSession(sessionId);

        // 校验签到状态
        if ("ENDED".equals(session.getStatus())) {
            throw BusinessException.badRequest("签到已结束");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartTime())) {
            throw BusinessException.badRequest("签到尚未开始");
        }
        if (now.isAfter(session.getEndTime())) {
            throw BusinessException.badRequest("签到已截止");
        }

        // 校验选课
        Long enrolled = courseStudentMapper.selectCount(
                new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getCourseId, session.getCourseId())
                        .eq(CourseStudent::getStudentId, studentId));
        if (enrolled == null || enrolled == 0) {
            throw BusinessException.badRequest("你未选修该课程");
        }

        // 校验是否已签到
        Long existCount = recordMapper.selectCount(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getSessionId, sessionId)
                        .eq(CheckinRecord::getStudentId, studentId));
        if (existCount != null && existCount > 0) {
            throw BusinessException.badRequest("你已签到，无需重复签到");
        }

        // 根据签到方式校验
        String checkinMethod = session.getCheckinType();
        if ("NUMERIC_CODE".equals(checkinMethod)) {
            // 数字码校验
            if (!StringUtils.hasText(dto.getCode())) {
                throw BusinessException.badRequest("请输入签到码");
            }
            if (!dto.getCode().equalsIgnoreCase(session.getCode())) {
                throw BusinessException.badRequest("签到码错误");
            }
        } else if ("LOCATION".equals(checkinMethod)) {
            // 地理位置校验
            if (dto.getLatitude() == null || dto.getLongitude() == null) {
                throw BusinessException.badRequest("无法获取位置信息，请允许定位权限");
            }
            double distance = haversineDistance(
                    session.getLatitude().doubleValue(), session.getLongitude().doubleValue(),
                    dto.getLatitude().doubleValue(), dto.getLongitude().doubleValue());
            int radius = session.getLocationRadius() != null ? session.getLocationRadius() : 100;
            if (distance > radius) {
                throw BusinessException.badRequest(
                        String.format("你距离签到点约%.0f米，超出有效范围(%d米)", distance, radius));
            }
        }

        // 判断是否迟到（前80%为正常，后20%为迟到窗口）
        long totalSeconds = ChronoUnit.SECONDS.between(session.getStartTime(), session.getEndTime());
        long elapsedSeconds = ChronoUnit.SECONDS.between(session.getStartTime(), now);
        String recordStatus = elapsedSeconds <= totalSeconds * 0.8 ? "SUCCESS" : "LATE";

        // 写入签到记录
        CheckinRecord record = new CheckinRecord();
        record.setSessionId(sessionId);
        record.setStudentId(studentId);
        record.setCheckinTime(now);
        record.setCheckinMethod(checkinMethod);
        record.setLatitude(dto.getLatitude());
        record.setLongitude(dto.getLongitude());
        record.setStatus(recordStatus);
        recordMapper.insert(record);

        // 积分联动：成功签到+1分
        if ("SUCCESS".equals(recordStatus)) {
            awardCheckinPoint(session.getCourseId(), studentId, session.getCreateBy(), session.getTitle());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("checkinTime", now);
        result.put("status", recordStatus);
        result.put("message", "LATE".equals(recordStatus) ? "签到成功（迟到）" : "签到成功");
        return result;
    }

    /**
     * 课程维度出勤记录（分页）
     */
    public PageResult<Map<String, Object>> courseRecords(Long courseId, Long userId, String role, int page, int size) {
        if ("ROLE_TEACHER".equals(role)) {
            Course course = courseMapper.selectById(courseId);
            if (course == null || !Objects.equals(course.getTeacherId(), userId)) {
                throw BusinessException.forbidden("只能查看自己课程的出勤记录");
            }
        }

        Page<CheckinSession> mpPage = sessionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<CheckinSession>()
                        .eq(CheckinSession::getCourseId, courseId)
                        .orderByDesc(CheckinSession::getStartTime));

        List<Map<String, Object>> records = mpPage.getRecords().stream().map(session -> {
            Map<String, Object> map = toSessionMapWithStats(session);
            if ("ROLE_STUDENT".equals(role)) {
                CheckinRecord myRecord = recordMapper.selectOne(
                        new LambdaQueryWrapper<CheckinRecord>()
                                .eq(CheckinRecord::getSessionId, session.getId())
                                .eq(CheckinRecord::getStudentId, userId));
                map.put("myStatus", myRecord != null ? myRecord.getStatus() : "ABSENT");
            }
            return map;
        }).collect(Collectors.toList());

        return PageResult.of(records, mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize());
    }

    /**
     * 课程维度出勤统计
     */
    public Map<String, Object> courseAttendanceStats(Long courseId, Long teacherId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || !Objects.equals(course.getTeacherId(), teacherId)) {
            throw BusinessException.forbidden("只能查看自己课程的出勤统计");
        }

        List<CheckinSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<CheckinSession>()
                        .eq(CheckinSession::getCourseId, courseId)
                        .orderByDesc(CheckinSession::getStartTime));

        // 获取选课学生总数
        long totalStudents = courseStudentMapper.selectCount(
                new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getCourseId, courseId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseId", courseId);
        result.put("courseName", course.getCourseName());
        result.put("totalSessions", sessions.size());
        result.put("totalStudents", totalStudents);

        // 所有签到 session 的学生出勤汇总
        if (!sessions.isEmpty()) {
            List<Long> sessionIds = sessions.stream().map(CheckinSession::getId).collect(Collectors.toList());
            List<CheckinRecord> allRecords = recordMapper.selectList(
                    new LambdaQueryWrapper<CheckinRecord>().in(CheckinRecord::getSessionId, sessionIds));

            // 每个学生的出勤统计
            Map<Long, List<CheckinRecord>> studentRecords = allRecords.stream()
                    .collect(Collectors.groupingBy(CheckinRecord::getStudentId));

            List<Map<String, Object>> studentAttendance = studentRecords.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> s = studentInfo(entry.getKey());
                        long success = entry.getValue().stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
                        long late = entry.getValue().stream().filter(r -> "LATE".equals(r.getStatus())).count();
                        long absent = sessions.size() - success - late;
                        s.put("total", sessions.size());
                        s.put("success", success);
                        s.put("late", late);
                        s.put("absent", Math.max(0, absent));
                        s.put("attendanceRate", sessions.size() > 0
                                ? BigDecimal.valueOf((success + late) * 100.0 / sessions.size())
                                        .setScale(1, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO);
                        return s;
                    })
                    .sorted((a, b) -> ((BigDecimal) b.get("attendanceRate")).compareTo((BigDecimal) a.get("attendanceRate")))
                    .collect(Collectors.toList());

            result.put("studentAttendance", studentAttendance);

            // 整体统计
            long totalCheckins = allRecords.size();
            long totalSuccess = allRecords.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
            long totalLate = allRecords.stream().filter(r -> "LATE".equals(r.getStatus())).count();
            result.put("totalCheckins", totalCheckins);
            result.put("totalSuccess", totalSuccess);
            result.put("totalLate", totalLate);
        }

        return result;
    }

    // ==================== 定时任务 ====================
    /**
     * 每分钟扫描过期签到，自动标记缺勤
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoCloseExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<CheckinSession> expiredSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<CheckinSession>()
                        .eq(CheckinSession::getStatus, "ACTIVE")
                        .lt(CheckinSession::getEndTime, now));

        for (CheckinSession session : expiredSessions) {
            log.info("自动关闭过期签到: sessionId={}, courseId={}", session.getId(), session.getCourseId());
            endSession(session);
        }
    }

    // ==================== 私有方法 ====================
    private void endSession(CheckinSession session) {
        session.setStatus("ENDED");
        sessionMapper.updateById(session);

        // 标记未签到学生为缺勤（ABSENT）
        List<CourseStudent> enrolledStudents = courseStudentMapper.selectList(
                new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getCourseId, session.getCourseId()));

        Set<Long> checkedInStudentIds = recordMapper.selectList(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getSessionId, session.getId()))
                .stream().map(CheckinRecord::getStudentId).collect(Collectors.toSet());

        for (CourseStudent cs : enrolledStudents) {
            if (!checkedInStudentIds.contains(cs.getStudentId())) {
                CheckinRecord absentRecord = new CheckinRecord();
                absentRecord.setSessionId(session.getId());
                absentRecord.setStudentId(cs.getStudentId());
                absentRecord.setCheckinTime(session.getEndTime());
                absentRecord.setStatus("ABSENT");
                recordMapper.insert(absentRecord);
            }
        }
    }

    private CheckinSession getSession(Long sessionId) {
        CheckinSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw BusinessException.notFound("签到场次不存在");
        }
        return session;
    }

    /**
     * 生成6位随机数字签到码
     */
    private String generateCode() {
        int code = 100000 + new Random().nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Haversine公式计算两点距离（米）
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // 地球半径（米）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 签到成功积分奖励
     */
    private void awardCheckinPoint(Long courseId, Long studentId, Long teacherId, String sessionTitle) {
        try {
            ParticipationPoint point = new ParticipationPoint();
            point.setCourseId(courseId);
            point.setStudentId(studentId);
            point.setTeacherId(teacherId);
            point.setPoints(1);
            point.setSourceType("课堂签到");
            point.setReason("签到: " + (StringUtils.hasText(sessionTitle) ? sessionTitle : "课堂签到"));
            point.setEventTime(LocalDateTime.now());
            pointMapper.insert(point);
        } catch (Exception e) {
            log.warn("签到积分写入失败: studentId={}, sessionTitle={}", studentId, sessionTitle, e);
        }
    }

    /**
     * 构建出勤统计
     */
    private Map<String, Object> buildStats(CheckinSession session, List<CheckinRecord> records) {
        Map<String, Object> stats = new LinkedHashMap<>();
        long totalStudents = courseStudentMapper.selectCount(
                new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getCourseId, session.getCourseId()));
        long successCount = records.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        long lateCount = records.stream().filter(r -> "LATE".equals(r.getStatus())).count();
        long absentCount = totalStudents - successCount - lateCount;

        stats.put("totalStudents", totalStudents);
        stats.put("successCount", successCount);
        stats.put("lateCount", lateCount);
        stats.put("absentCount", Math.max(0, absentCount));
        stats.put("checkinRate", totalStudents > 0
                ? BigDecimal.valueOf((successCount + lateCount) * 100.0 / totalStudents)
                        .setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // 学生名单（含签到状态）
        List<CourseStudent> enrolledStudents = courseStudentMapper.selectList(
                new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getCourseId, session.getCourseId()));
        Map<Long, CheckinRecord> recordMap = records.stream()
                .collect(Collectors.toMap(CheckinRecord::getStudentId, r -> r, (a, b) -> a));

        // 已签到学生名单
        List<Map<String, Object>> checkedInStudents = records.stream()
                .map(r -> {
                    Map<String, Object> s = studentInfo(r.getStudentId());
                    s.put("checkinTime", r.getCheckinTime());
                    s.put("status", r.getStatus());
                    s.put("checkinMethod", r.getCheckinMethod());
                    return s;
                })
                .sorted((a, b) -> {
                    String sa = (String) a.getOrDefault("status", "");
                    String sb = (String) b.getOrDefault("status", "");
                    if ("SUCCESS".equals(sa) && !"SUCCESS".equals(sb)) {
                        return -1;
                    }
                    if (!"SUCCESS".equals(sa) && "SUCCESS".equals(sb)) {
                        return 1;
                    }
                    return 0;
                })
                .collect(Collectors.toList());
        stats.put("checkedInStudents", checkedInStudents);

        // 缺勤学生名单
        List<Map<String, Object>> absentStudents = enrolledStudents.stream()
                .filter(cs -> !recordMap.containsKey(cs.getStudentId()))
                .map(cs -> {
                    Map<String, Object> s = studentInfo(cs.getStudentId());
                    s.put("status", "ABSENT");
                    return s;
                })
                .collect(Collectors.toList());
        stats.put("absentStudents", absentStudents);

        return stats;
    }

    /**
     * 查询学生基本信息
     */
    private Map<String, Object> studentInfo(Long userId) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("studentId", userId);
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            info.put("studentName", StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            info.put("avatar", user.getAvatar());
        }
        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, userId));
        if (profile != null) {
            info.put("studentNo", profile.getStudentNo());
            info.put("realName", profile.getRealName());
            info.put("className", profile.getClassName());
        }
        return info;
    }

    /**
     * Session转Map（基础信息，不含敏感数据）
     */
    private Map<String, Object> toSessionMap(CheckinSession session, String courseName) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", session.getId());
        map.put("courseId", session.getCourseId());
        map.put("courseName", courseName);
        map.put("title", session.getTitle());
        map.put("checkinType", session.getCheckinType());
        map.put("code", session.getCode());
        map.put("latitude", session.getLatitude());
        map.put("longitude", session.getLongitude());
        map.put("locationRadius", session.getLocationRadius());
        map.put("startTime", session.getStartTime());
        map.put("endTime", session.getEndTime());
        map.put("status", session.getStatus());
        map.put("createBy", session.getCreateBy());
        map.put("createTime", session.getCreateTime());
        return map;
    }

    /**
     * Session转Map（含统计摘要）
     */
    private Map<String, Object> toSessionMapWithStats(CheckinSession session) {
        Course course = courseMapper.selectById(session.getCourseId());
        Map<String, Object> map = toSessionMap(session, course != null ? course.getCourseName() : null);

        long totalStudents = courseStudentMapper.selectCount(
                new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getCourseId, session.getCourseId()));
        List<CheckinRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<CheckinRecord>()
                        .eq(CheckinRecord::getSessionId, session.getId()));
        long successCount = records.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        long lateCount = records.stream().filter(r -> "LATE".equals(r.getStatus())).count();

        map.put("totalStudents", totalStudents);
        map.put("checkedInCount", successCount + lateCount);
        map.put("successCount", successCount);
        map.put("lateCount", lateCount);
        return map;
    }

    /**
     * Record转Map
     */
    private Map<String, Object> toRecordMap(CheckinRecord record, CheckinSession session) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", record.getId());
        map.put("sessionId", record.getSessionId());
        map.put("student", studentInfo(record.getStudentId()));
        map.put("checkinTime", record.getCheckinTime());
        map.put("checkinMethod", record.getCheckinMethod());
        map.put("status", record.getStatus());
        return map;
    }
}
