package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 仪表盘服务
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CourseMapper courseMapper;
    private final AssignmentSubmitMapper submitMapper;
    private final AssignmentMapper assignmentMapper;
    private final QuestionPostMapper postMapper;
    private final ExamRecordMapper recordMapper;
    private final ExamMapper examMapper;
    private final GradeCourseMapper gradeCourseMapper;
    private final CourseStudentMapper courseStudentMapper;

    public DashboardVO getTeacherDashboard(Long teacherId) {
        DashboardVO vo = new DashboardVO();
        // 课程数
        vo.setCourseCount(courseMapper.selectCount(
                new LambdaQueryWrapper<Course>().eq(Course::getTeacherId, teacherId)));
        // 待批改作业（只统计该教师课程的作业）
        List<Course> teacherCourses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getTeacherId, teacherId));
        List<Long> courseIds = teacherCourses.stream().map(Course::getId).collect(java.util.stream.Collectors.toList());
        long pendingSubmits = 0;
        if (!courseIds.isEmpty()) {
            List<Assignment> assignments = assignmentMapper.selectList(
                    new LambdaQueryWrapper<Assignment>().in(Assignment::getCourseId, courseIds));
            List<Long> assignmentIds = assignments.stream().map(Assignment::getId).collect(java.util.stream.Collectors.toList());
            if (!assignmentIds.isEmpty()) {
                pendingSubmits = submitMapper.selectCount(
                        new LambdaQueryWrapper<AssignmentSubmit>()
                                .in(AssignmentSubmit::getAssignmentId, assignmentIds)
                                .eq(AssignmentSubmit::getStatus, "待批改"));
            }
        }
        vo.setPendingAssignmentCount(pendingSubmits);
        // 待回复问题
        vo.setPendingQuestionCount(postMapper.selectCount(
                new LambdaQueryWrapper<QuestionPost>().eq(QuestionPost::getReplyCount, 0)));
        // 近7日活跃趋势（模拟数据）
        List<DashboardVO.DailyActive> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            DashboardVO.DailyActive da = new DashboardVO.DailyActive();
            da.setDate(LocalDate.now().minusDays(i).toString());
            da.setCount((int) (Math.random() * 50 + 20));
            trend.add(da);
        }
        vo.setActiveTrend(trend);
        return vo;
    }

    public DashboardVO getStudentDashboard(Long studentId) {
        DashboardVO vo = new DashboardVO();
        // 在学课程数
        long courseCount = courseStudentMapper.selectCount(
                new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getStudentId, studentId));
        vo.setCourseCount(courseCount);

        // 获取该学生已选课程的ID列表
        List<CourseStudent> enrollments = courseStudentMapper.selectList(
                new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getStudentId, studentId));
        List<Long> enrolledCourseIds = enrollments.stream().map(CourseStudent::getCourseId).collect(java.util.stream.Collectors.toList());

        // 待做作业数
        long pendingAssignments = 0;
        if (!enrolledCourseIds.isEmpty()) {
            List<Assignment> allAssignments = assignmentMapper.selectList(
                    new LambdaQueryWrapper<Assignment>().in(Assignment::getCourseId, enrolledCourseIds));
            for (Assignment a : allAssignments) {
                // 检查是否已提交
                AssignmentSubmit sub = submitMapper.selectOne(
                        new LambdaQueryWrapper<AssignmentSubmit>()
                                .eq(AssignmentSubmit::getAssignmentId, a.getId())
                                .eq(AssignmentSubmit::getStudentId, studentId));
                if (sub == null) {
                    pendingAssignments++;
                }
            }
        }
        vo.setPendingAssignmentCount(pendingAssignments);

        // 待考试数
        long upcomingExams = 0;
        if (!enrolledCourseIds.isEmpty()) {
            upcomingExams = examMapper.selectCount(
                    new LambdaQueryWrapper<Exam>()
                            .in(Exam::getCourseId, enrolledCourseIds)
                            .eq(Exam::getStatus, "进行中"));
        }
        vo.setUpcomingExamCount(upcomingExams);

        // 学习趋势（模拟，后续可接入真实数据）
        List<DashboardVO.DailyActive> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            DashboardVO.DailyActive da = new DashboardVO.DailyActive();
            da.setDate(LocalDate.now().minusDays(i).toString());
            da.setCount((int) (Math.random() * 8 + 2));
            trend.add(da);
        }
        vo.setActiveTrend(trend);
        return vo;
    }
}
