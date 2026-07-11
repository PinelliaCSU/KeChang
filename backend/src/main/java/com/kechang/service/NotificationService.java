package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.dto.AnnouncementDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知服务（含公告功能）
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notifMapper;
    private final NotificationUserMapper notifUserMapper;
    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final StudentProfileMapper studentProfileMapper;

    // ==================== 通用通知（仅平台公告） ====================
    public PageResult<Map<String, Object>> list(Long userId, int page, int size) {
        List<NotificationUser> nus = notifUserMapper.selectList(
                new LambdaQueryWrapper<NotificationUser>().eq(NotificationUser::getUserId, userId));
        if (nus.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        Map<Long, Integer> readMap = nus.stream()
                .collect(Collectors.toMap(NotificationUser::getNotificationId, NotificationUser::getIsRead));
        List<Long> ids = nus.stream().map(NotificationUser::getNotificationId).toList();
        // 仅返回平台公告，不包含作业/考试/实训等
        Page<Notification> p = notifMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .in(Notification::getId, ids)
                        .eq(Notification::getNotificationType, "公告")
                        .and(w -> w.isNull(Notification::getCourseId)
                                .or().eq(Notification::getCourseId, SCOPE_ALL_TEACHERS)
                                .or().eq(Notification::getCourseId, SCOPE_ALL_STUDENTS))
                        .orderByDesc(Notification::getSendTime));

        List<Map<String, Object>> records = new ArrayList<>();
        for (Notification n : p.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", n.getId());
            item.put("title", n.getTitle());
            item.put("content", n.getContent());
            item.put("senderId", n.getSenderId());
            item.put("sendTime", n.getSendTime());
            item.put("notificationType", n.getNotificationType());
            item.put("courseId", n.getCourseId());
            item.put("createTime", n.getCreateTime());
            item.put("isRead", readMap.getOrDefault(n.getId(), 0));
            records.add(item);
        }
        return PageResult.of(records, p.getTotal(), page, size);
    }

    /**
     * 获取通知详情
     */
    public Map<String, Object> getDetail(Long id, Long userId) {
        Notification n = notifMapper.selectById(id);
        if (n == null) {
            throw BusinessException.notFound("通知不存在");
        }

        Map<String, Object> item = new HashMap<>();
        item.put("id", n.getId());
        item.put("title", n.getTitle());
        item.put("content", n.getContent());
        item.put("senderId", n.getSenderId());
        item.put("sendTime", n.getSendTime());
        item.put("notificationType", n.getNotificationType());
        item.put("courseId", n.getCourseId());
        item.put("createTime", n.getCreateTime());

        NotificationUser nu = notifUserMapper.selectOne(new LambdaQueryWrapper<NotificationUser>()
                .eq(NotificationUser::getNotificationId, id)
                .eq(NotificationUser::getUserId, userId));
        item.put("isRead", nu != null ? nu.getIsRead() : 0);

        return item;
    }

    public long unreadCount(Long userId) {
        // 仅统计管理端平台公告的未读数，不包含作业/考试/实训等通知
        Long count = notifUserMapper.countUnreadAnnouncements(userId);
        return count != null ? count : 0;
    }

    public void markRead(Long id, Long userId) {
        NotificationUser nu = notifUserMapper.selectOne(new LambdaQueryWrapper<NotificationUser>()
                .eq(NotificationUser::getNotificationId, id).eq(NotificationUser::getUserId, userId));
        if (nu != null) {
            nu.setIsRead(1);
            nu.setReadTime(LocalDateTime.now());
            notifUserMapper.updateById(nu);
        }
    }

    public void markAllRead(Long userId) {
        List<NotificationUser> nus = notifUserMapper.selectList(new LambdaQueryWrapper<NotificationUser>()
                .eq(NotificationUser::getUserId, userId).eq(NotificationUser::getIsRead, 0));
        nus.forEach(nu -> {
            nu.setIsRead(1);
            nu.setReadTime(LocalDateTime.now());
            notifUserMapper.updateById(nu);
        });
    }

    public void send(Notification n) {
        notifMapper.insert(n);
    }

    @Transactional
    public void sendToCourseStudents(Long courseId, Long senderId, String type, String title, String content) {
        if (courseId == null) {
            return;
        }
        Notification n = new Notification();
        n.setTitle(title);
        n.setContent(content);
        n.setSenderId(senderId);
        n.setSendTime(LocalDateTime.now());
        n.setNotificationType(type);
        n.setCourseId(courseId);
        notifMapper.insert(n);

        List<CourseStudent> students = courseStudentMapper.selectList(
                new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getCourseId, courseId));
        for (CourseStudent cs : students) {
            NotificationUser nu = new NotificationUser();
            nu.setNotificationId(n.getId());
            nu.setUserId(cs.getStudentId());
            nu.setIsRead(0);
            notifUserMapper.insert(nu);
        }
    }

    @Transactional
    public void sendToUser(Long userId, Long senderId, Long courseId, String type, String title, String content) {
        if (userId == null) {
            return;
        }
        Notification n = new Notification();
        n.setTitle(title);
        n.setContent(content);
        n.setSenderId(senderId);
        n.setSendTime(LocalDateTime.now());
        n.setNotificationType(type);
        n.setCourseId(courseId);
        notifMapper.insert(n);

        NotificationUser nu = new NotificationUser();
        nu.setNotificationId(n.getId());
        nu.setUserId(userId);
        nu.setIsRead(0);
        notifUserMapper.insert(nu);
    }

    @Transactional
    public void sendToRole(String roleCode, Long senderId, String type, String title, String content) {
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        if (role == null) {
            return;
        }
        Notification n = new Notification();
        n.setTitle(title);
        n.setContent(content);
        n.setSenderId(senderId);
        n.setSendTime(LocalDateTime.now());
        n.setNotificationType(type);
        notifMapper.insert(n);

        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, role.getId())
                .eq(SysUser::getStatus, 0));
        for (SysUser user : users) {
            NotificationUser nu = new NotificationUser();
            nu.setNotificationId(n.getId());
            nu.setUserId(user.getId());
            nu.setIsRead(0);
            notifUserMapper.insert(nu);
        }
    }

    // ==================== 公告功能 ====================
    /**
     * 分页查询公告列表 - 教师：查看自己发布的公告 - 学生：查看已选课程的公告，附带确认状态
     */
    public PageResult<Map<String, Object>> listAnnouncements(int page, int size, Long courseId, Long userId,
            String role) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getNotificationType, "公告");

        if ("ROLE_TEACHER".equals(role)) {
            // 教师只看自己的公告
            wrapper.eq(Notification::getSenderId, userId);
        } else {
            // 学生只看已选课程的公告
            List<CourseStudent> enrolled = courseStudentMapper.selectList(
                    new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getStudentId, userId));
            if (enrolled.isEmpty()) {
                return PageResult.of(List.of(), 0, page, size);
            }
            List<Long> courseIds = enrolled.stream().map(CourseStudent::getCourseId).distinct().toList();
            wrapper.in(Notification::getCourseId, courseIds);
        }

        if (courseId != null) {
            wrapper.eq(Notification::getCourseId, courseId);
        }
        wrapper.orderByDesc(Notification::getSendTime);

        Page<Notification> p = notifMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Notification n : p.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", n.getId());
            item.put("title", n.getTitle());
            item.put("content", n.getContent());
            item.put("courseId", n.getCourseId());
            item.put("publisherId", n.getSenderId());
            item.put("publishTime", n.getSendTime());
            item.put("createTime", n.getCreateTime());

            // 课程名
            if (n.getCourseId() != null) {
                Course c = courseMapper.selectById(n.getCourseId());
                item.put("courseName", c != null ? c.getCourseName() : "");
            } else {
                item.put("courseName", "");
            }

            // 发布教师名
            if (n.getSenderId() != null) {
                SysUser u = sysUserMapper.selectById(n.getSenderId());
                item.put("publisherName", u != null ? u.getNickname() : "");
            } else {
                item.put("publisherName", "");
            }

            // 确认人数
            long confirmCount = notifUserMapper.selectCount(new LambdaQueryWrapper<NotificationUser>()
                    .eq(NotificationUser::getNotificationId, n.getId())
                    .eq(NotificationUser::getIsRead, 1));
            item.put("confirmCount", (int) confirmCount);

            // 当前学生是否已确认
            if ("ROLE_STUDENT".equals(role)) {
                NotificationUser nu = notifUserMapper.selectOne(new LambdaQueryWrapper<NotificationUser>()
                        .eq(NotificationUser::getNotificationId, n.getId())
                        .eq(NotificationUser::getUserId, userId));
                item.put("confirmed", nu != null && nu.getIsRead() != null && nu.getIsRead() == 1);
            } else {
                item.put("confirmed", false);
            }

            records.add(item);
        }
        return PageResult.of(records, p.getTotal(), page, size);
    }

    /**
     * 获取公告详情
     */
    public Map<String, Object> getAnnouncementDetail(Long id, Long userId, String role) {
        Notification n = notifMapper.selectById(id);
        if (n == null || !"公告".equals(n.getNotificationType())) {
            throw BusinessException.notFound("公告不存在");
        }

        Map<String, Object> item = new HashMap<>();
        item.put("id", n.getId());
        item.put("title", n.getTitle());
        item.put("content", n.getContent());
        item.put("courseId", n.getCourseId());
        item.put("publisherId", n.getSenderId());
        item.put("publishTime", n.getSendTime());
        item.put("createTime", n.getCreateTime());

        if (n.getCourseId() != null) {
            Course c = courseMapper.selectById(n.getCourseId());
            item.put("courseName", c != null ? c.getCourseName() : "");
        } else {
            item.put("courseName", "");
        }

        if (n.getSenderId() != null) {
            SysUser u = sysUserMapper.selectById(n.getSenderId());
            item.put("publisherName", u != null ? u.getNickname() : "");
        } else {
            item.put("publisherName", "");
        }

        long confirmCount = notifUserMapper.selectCount(new LambdaQueryWrapper<NotificationUser>()
                .eq(NotificationUser::getNotificationId, n.getId())
                .eq(NotificationUser::getIsRead, 1));
        item.put("confirmCount", (int) confirmCount);

        if ("ROLE_STUDENT".equals(role)) {
            NotificationUser nu = notifUserMapper.selectOne(new LambdaQueryWrapper<NotificationUser>()
                    .eq(NotificationUser::getNotificationId, n.getId())
                    .eq(NotificationUser::getUserId, userId));
            item.put("confirmed", nu != null && nu.getIsRead() != null && nu.getIsRead() == 1);
        } else {
            item.put("confirmed", false);
        }

        // 权限校验：学生只能看已选课程的公告
        if ("ROLE_STUDENT".equals(role) && n.getCourseId() != null) {
            CourseStudent cs = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, n.getCourseId())
                    .eq(CourseStudent::getStudentId, userId));
            if (cs == null) {
                throw BusinessException.forbidden("您未选修该课程，无法查看此公告");
            }
        }

        return item;
    }

    /**
     * 发布公告：插入 notification + 为课程所有学生创建 notification_user 记录
     */
    @Transactional
    public void createAnnouncement(AnnouncementDTO dto, Long teacherId) {
        Notification n = new Notification();
        n.setTitle(dto.getTitle());
        n.setContent(dto.getContent());
        n.setCourseId(dto.getCourseId());
        n.setSenderId(teacherId);
        n.setSendTime(LocalDateTime.now());
        n.setNotificationType("公告");
        notifMapper.insert(n);

        // 为课程所有学生创建 notification_user 记录
        List<CourseStudent> students = courseStudentMapper.selectList(
                new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getCourseId, dto.getCourseId()));
        for (CourseStudent cs : students) {
            NotificationUser nu = new NotificationUser();
            nu.setNotificationId(n.getId());
            nu.setUserId(cs.getStudentId());
            nu.setIsRead(0);
            notifUserMapper.insert(nu);
        }
    }

    /**
     * 编辑公告（仅发布者本人可编辑）
     */
    public void updateAnnouncement(Long id, AnnouncementDTO dto, Long userId) {
        Notification n = notifMapper.selectById(id);
        if (n == null || !"公告".equals(n.getNotificationType())) {
            throw BusinessException.notFound("公告不存在");
        }
        if (!n.getSenderId().equals(userId)) {
            throw BusinessException.forbidden("只能编辑自己发布的公告");
        }
        n.setTitle(dto.getTitle());
        n.setContent(dto.getContent());
        n.setCourseId(dto.getCourseId());
        notifMapper.updateById(n);
    }

    /**
     * 删除公告（逻辑删除，仅发布者本人可删）
     */
    @Transactional
    public void deleteAnnouncement(Long id, Long userId) {
        Notification n = notifMapper.selectById(id);
        if (n == null || !"公告".equals(n.getNotificationType())) {
            throw BusinessException.notFound("公告不存在");
        }
        if (!n.getSenderId().equals(userId)) {
            throw BusinessException.forbidden("只能删除自己发布的公告");
        }
        notifMapper.deleteById(id);

        // 同时删除关联的 notification_user 记录
        List<NotificationUser> nus = notifUserMapper.selectList(
                new LambdaQueryWrapper<NotificationUser>().eq(NotificationUser::getNotificationId, id));
        for (NotificationUser nu : nus) {
            notifUserMapper.deleteById(nu.getId());
        }
    }

    /**
     * 删除通知（逻辑删除，只能删除自己的通知）
     */
    @Transactional
    public void deleteNotification(Long id, Long userId) {
        NotificationUser nu = notifUserMapper.selectOne(
                new LambdaQueryWrapper<NotificationUser>()
                        .eq(NotificationUser::getNotificationId, id)
                        .eq(NotificationUser::getUserId, userId));
        if (nu != null) {
            notifUserMapper.deleteById(nu.getId());
            notifMapper.deleteById(id);
        }
    }

    /**
     * 清空当前用户所有通知
     */
    @Transactional
    public void clearAllNotifications(Long userId) {
        List<NotificationUser> nus = notifUserMapper.selectList(
                new LambdaQueryWrapper<NotificationUser>()
                        .eq(NotificationUser::getUserId, userId));
        for (NotificationUser nu : nus) {
            notifUserMapper.deleteById(nu.getId());
        }

        List<Long> notifIds = nus.stream().map(NotificationUser::getNotificationId).distinct().toList();
        for (Long id : notifIds) {
            notifMapper.deleteById(id);
        }
    }

    /**
     * 学生确认公告
     */
    public void confirmAnnouncement(Long announcementId, Long studentId) {
        Notification n = notifMapper.selectById(announcementId);
        if (n == null || !"公告".equals(n.getNotificationType())) {
            throw BusinessException.notFound("公告不存在");
        }

        // 校验学生已选该课程
        if (n.getCourseId() != null) {
            CourseStudent cs = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, n.getCourseId())
                    .eq(CourseStudent::getStudentId, studentId));
            if (cs == null) {
                throw BusinessException.forbidden("您未选修该课程，无法确认此公告");
            }
        }

        // 查找或创建 notification_user 记录
        NotificationUser nu = notifUserMapper.selectOne(new LambdaQueryWrapper<NotificationUser>()
                .eq(NotificationUser::getNotificationId, announcementId)
                .eq(NotificationUser::getUserId, studentId));
        if (nu == null) {
            // 如果该学生还没有记录（可能是在创建公告后才选课），则创建一条
            nu = new NotificationUser();
            nu.setNotificationId(announcementId);
            nu.setUserId(studentId);
            nu.setIsRead(1);
            nu.setReadTime(LocalDateTime.now());
            notifUserMapper.insert(nu);
        } else if (nu.getIsRead() != null && nu.getIsRead() == 1) {
            throw BusinessException.badRequest("您已确认过该公告");
        } else {
            nu.setIsRead(1);
            nu.setReadTime(LocalDateTime.now());
            notifUserMapper.updateById(nu);
        }
    }

    /**
     * 教师查看公告确认列表（含学生姓名学号）
     */
    public PageResult<Map<String, Object>> getConfirmList(Long announcementId, int page, int size, Long userId) {
        Notification n = notifMapper.selectById(announcementId);
        if (n == null || !"公告".equals(n.getNotificationType())) {
            throw BusinessException.notFound("公告不存在");
        }
        if (!n.getSenderId().equals(userId)) {
            throw BusinessException.forbidden("只能查看自己发布的公告的确认情况");
        }

        Page<NotificationUser> p = notifUserMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<NotificationUser>()
                        .eq(NotificationUser::getNotificationId, announcementId)
                        .eq(NotificationUser::getIsRead, 1)
                        .orderByAsc(NotificationUser::getReadTime));

        List<Map<String, Object>> records = new ArrayList<>();
        for (NotificationUser nu : p.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", nu.getId());
            item.put("studentId", nu.getUserId());
            item.put("confirmTime", nu.getReadTime());

            // 学生信息
            SysUser u = sysUserMapper.selectById(nu.getUserId());
            item.put("studentName", u != null ? u.getNickname() : "");
            item.put("username", u != null ? u.getUsername() : "");

            StudentProfile sp = studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, nu.getUserId()));
            item.put("studentNo", sp != null ? sp.getStudentNo() : "");

            records.add(item);
        }
        return PageResult.of(records, p.getTotal(), page, size);
    }

    /**
     * 获取公告的确认人数
     */
    public int getConfirmCount(Long announcementId) {
        Long count = notifUserMapper.selectCount(new LambdaQueryWrapper<NotificationUser>()
                .eq(NotificationUser::getNotificationId, announcementId)
                .eq(NotificationUser::getIsRead, 1));
        return count != null ? count.intValue() : 0;
    }

    // ==================== 管理端公告功能 ====================

    /** courseId 特殊值：所有教师 */
    private static final Long SCOPE_ALL_TEACHERS = -1L;
    /** courseId 特殊值：所有学生 */
    private static final Long SCOPE_ALL_STUDENTS = -2L;

    /**
     * 管理员创建平台公告（支持按范围发送：全平台/所有教师/所有学生）
     */
    @Transactional
    public void createPlatformAnnouncement(AnnouncementDTO dto, Long senderId) {
        Notification n = new Notification();
        n.setTitle(dto.getTitle());
        n.setContent(dto.getContent());
        n.setSenderId(senderId);
        n.setSendTime(LocalDateTime.now());
        n.setNotificationType("公告");

        String scope = dto.getScope() != null ? dto.getScope() : "all";
        List<SysUser> targetUsers;
        switch (scope) {
            case "teachers":
                n.setCourseId(SCOPE_ALL_TEACHERS);
                targetUsers = findUsersByRoleCode("ROLE_TEACHER");
                break;
            case "students":
                n.setCourseId(SCOPE_ALL_STUDENTS);
                targetUsers = findUsersByRoleCode("ROLE_STUDENT");
                break;
            default: // "all"
                n.setCourseId(null);
                targetUsers = findUsersByRoleCodes("ROLE_TEACHER", "ROLE_STUDENT");
                break;
        }

        notifMapper.insert(n);

        for (SysUser u : targetUsers) {
            NotificationUser nu = new NotificationUser();
            nu.setNotificationId(n.getId());
            nu.setUserId(u.getId());
            nu.setIsRead(0);
            notifUserMapper.insert(nu);
        }
    }

    private List<SysUser> findUsersByRoleCode(String roleCode) {
        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        if (role == null) return List.of();
        return sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, role.getId())
                        .eq(SysUser::getStatus, 0));
    }

    private List<SysUser> findUsersByRoleCodes(String... roleCodes) {
        List<SysRole> roles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().in(SysRole::getRoleCode, List.of(roleCodes)));
        if (roles.isEmpty()) return List.of();
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        return sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().in(SysUser::getRoleId, roleIds)
                        .eq(SysUser::getStatus, 0));
    }

    /**
     * 管理员分页查看所有公告（含课程公告和平台公告）
     */
    public PageResult<Map<String, Object>> listAllAnnouncements(int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getNotificationType, "公告")
                .orderByDesc(Notification::getSendTime);

        Page<Notification> p = notifMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Notification n : p.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", n.getId());
            item.put("title", n.getTitle());
            item.put("content", n.getContent());
            item.put("courseId", n.getCourseId());
            item.put("publisherId", n.getSenderId());
            item.put("publishTime", n.getSendTime());
            item.put("createTime", n.getCreateTime());

            // 范围
            if (SCOPE_ALL_TEACHERS.equals(n.getCourseId())) {
                item.put("courseName", "");
                item.put("scope", "所有教师");
            } else if (SCOPE_ALL_STUDENTS.equals(n.getCourseId())) {
                item.put("courseName", "");
                item.put("scope", "所有学生");
            } else if (n.getCourseId() != null) {
                Course c = courseMapper.selectById(n.getCourseId());
                item.put("courseName", c != null ? c.getCourseName() : "");
                item.put("scope", c != null ? c.getCourseName() : "课程");
            } else {
                item.put("courseName", "");
                item.put("scope", "全平台");
            }

            // 发布人
            if (n.getSenderId() != null) {
                SysUser u = sysUserMapper.selectById(n.getSenderId());
                item.put("publisherName", u != null ? u.getNickname() : "");
            } else {
                item.put("publisherName", "");
            }

            // 确认人数
            long confirmCount = notifUserMapper.selectCount(new LambdaQueryWrapper<NotificationUser>()
                    .eq(NotificationUser::getNotificationId, n.getId())
                    .eq(NotificationUser::getIsRead, 1));
            item.put("confirmCount", (int) confirmCount);

            records.add(item);
        }
        return PageResult.of(records, p.getTotal(), page, size);
    }

    /**
     * 管理员删除任意公告
     */
    @Transactional
    public void deletePlatformAnnouncement(Long id) {
        Notification n = notifMapper.selectById(id);
        if (n == null || !"公告".equals(n.getNotificationType())) {
            throw BusinessException.notFound("公告不存在");
        }
        notifMapper.deleteById(id);

        List<NotificationUser> nus = notifUserMapper.selectList(
                new LambdaQueryWrapper<NotificationUser>().eq(NotificationUser::getNotificationId, id));
        for (NotificationUser nu : nus) {
            notifUserMapper.deleteById(nu.getId());
        }
    }
}
