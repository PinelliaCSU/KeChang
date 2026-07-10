package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kechang.common.BusinessException;
import com.kechang.dto.ParticipationAwardDTO;
import com.kechang.dto.RollCallDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationPointMapper pointMapper;
    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final SysUserMapper sysUserMapper;

    public Map<String, Object> rollCall(RollCallDTO dto, Long teacherId) {
        Course course = teacherCourse(dto.getCourseId(), teacherId);
        List<CourseStudent> students = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, dto.getCourseId()));
        if (students.isEmpty()) {
            throw BusinessException.badRequest("该课程暂无选课学生");
        }
        int count = dto.getCount() == null || dto.getCount() <= 0 ? 1 : Math.min(dto.getCount(), students.size());
        Collections.shuffle(students);
        List<Map<String, Object>> selected = students.stream()
                .limit(count)
                .map(item -> withCoursePoint(studentInfo(item.getStudentId()), course.getId(), course.getCourseName()))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseId", course.getId());
        result.put("courseName", course.getCourseName());
        result.put("students", selected);
        return result;
    }

    public Map<String, Object> award(ParticipationAwardDTO dto, Long teacherId) {
        Course course = teacherCourse(dto.getCourseId(), teacherId);
        if (dto.getStudentId() == null) {
            throw BusinessException.badRequest("请选择学生");
        }
        Long exists = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, dto.getCourseId())
                .eq(CourseStudent::getStudentId, dto.getStudentId()));
        if (exists == null || exists == 0) {
            throw BusinessException.badRequest("该学生未选当前课程");
        }
        int points = dto.getPoints() == null ? 1 : dto.getPoints();
        if (points == 0 || points < -10 || points > 10) {
            throw BusinessException.badRequest("积分范围应为 -10 到 10，且不能为 0");
        }
        ParticipationPoint record = new ParticipationPoint();
        record.setCourseId(dto.getCourseId());
        record.setStudentId(dto.getStudentId());
        record.setTeacherId(teacherId);
        record.setPoints(points);
        record.setSourceType("随机点名");
        record.setReason(StringUtils.hasText(dto.getReason()) ? dto.getReason() : defaultReason(points));
        record.setEventTime(LocalDateTime.now());
        pointMapper.insert(record);

        Map<String, Object> result = studentInfo(dto.getStudentId());
        result.put("courseId", course.getId());
        result.put("courseName", course.getCourseName());
        result.put("points", points);
        result.put("reason", record.getReason());
        result.put("eventTime", record.getEventTime());
        result.put("coursePoints", coursePoints(dto.getStudentId(), course.getId()));
        return result;
    }

    public Map<String, Object> myOverview(Long studentId) {
        Map<String, Object> map = new LinkedHashMap<>();
        List<Map<String, Object>> records = myRecords(studentId);
        List<Map<String, Object>> courses = records.stream()
                .collect(Collectors.groupingBy(item -> item.get("courseId"), LinkedHashMap::new, Collectors.toList()))
                .values()
                .stream()
                .map(list -> {
                    Map<String, Object> first = list.get(0);
                    Map<String, Object> course = new LinkedHashMap<>();
                    course.put("courseId", first.get("courseId"));
                    course.put("courseName", first.get("courseName"));
                    course.put("totalPoints", list.stream().mapToInt(item -> (Integer) item.get("points")).sum());
                    course.put("records", list);
                    return course;
                })
                .collect(Collectors.toList());
        map.put("courseCount", courses.size());
        map.put("courses", courses);
        map.put("records", records);
        return map;
    }

    public List<Map<String, Object>> scoreboard(Long courseId, Long teacherId) {
        Course course = teacherCourse(courseId, teacherId);
        List<CourseStudent> students = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId));
        return students.stream()
                .map(item -> {
                    Map<String, Object> student = studentInfo(item.getStudentId());
                    student.put("courseId", course.getId());
                    student.put("courseName", course.getCourseName());
                    student.put("coursePoints", coursePoints(item.getStudentId(), courseId));
                    return student;
                })
                .sorted((a, b) -> Integer.compare((Integer) b.get("coursePoints"), (Integer) a.get("coursePoints")))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> myRecords(Long studentId) {
        return pointMapper.selectList(new LambdaQueryWrapper<ParticipationPoint>()
                        .eq(ParticipationPoint::getStudentId, studentId)
                        .orderByDesc(ParticipationPoint::getEventTime))
                .stream()
                .map(record -> {
                    Course course = courseMapper.selectById(record.getCourseId());
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", record.getId());
                    map.put("courseId", record.getCourseId());
                    map.put("courseName", course != null ? course.getCourseName() : "未知课程");
                    map.put("points", record.getPoints());
                    map.put("sourceType", record.getSourceType());
                    map.put("reason", record.getReason());
                    map.put("eventTime", record.getEventTime());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private int coursePoints(Long studentId, Long courseId) {
        return pointMapper.selectList(new LambdaQueryWrapper<ParticipationPoint>()
                        .eq(ParticipationPoint::getStudentId, studentId)
                        .eq(ParticipationPoint::getCourseId, courseId))
                .stream()
                .mapToInt(item -> item.getPoints() == null ? 0 : item.getPoints())
                .sum();
    }

    private Course teacherCourse(Long courseId, Long teacherId) {
        if (courseId == null) {
            throw BusinessException.badRequest("请选择课程");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        if (!Objects.equals(course.getTeacherId(), teacherId)) {
            throw BusinessException.forbidden("只能操作自己的课程");
        }
        return course;
    }

    private Map<String, Object> withCoursePoint(Map<String, Object> student, Long courseId, String courseName) {
        student.put("courseId", courseId);
        student.put("courseName", courseName);
        student.put("coursePoints", coursePoints((Long) student.get("studentId"), courseId));
        return student;
    }

    private String defaultReason(int points) {
        return points > 0 ? "课堂回答加分" : "课堂未到/未参与扣分";
    }

    private Map<String, Object> studentInfo(Long studentId) {
        StudentProfile profile = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, studentId)
                .last("limit 1"));
        SysUser user = sysUserMapper.selectById(studentId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("studentId", studentId);
        map.put("studentName", profile != null && StringUtils.hasText(profile.getRealName())
                ? profile.getRealName()
                : (user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "学生"));
        map.put("studentNo", profile != null ? profile.getStudentNo() : null);
        map.put("major", profile != null ? profile.getMajor() : null);
        map.put("className", profile != null ? profile.getClassName() : null);
        map.put("avatar", profile != null && StringUtils.hasText(profile.getAvatar())
                ? profile.getAvatar()
                : (user != null ? user.getAvatar() : null));
        return map;
    }
}
