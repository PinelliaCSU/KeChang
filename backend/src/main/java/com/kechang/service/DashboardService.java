package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.vo.AdminDashboardVO;
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
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final OperationLogMapper operationLogMapper;

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
        // 近7日活跃趋势：按该教师课程下的作业提交和考试提交统计
        List<DashboardVO.DailyActive> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long count = 0;
            if (!courseIds.isEmpty()) {
                List<Assignment> assignments = assignmentMapper.selectList(
                        new LambdaQueryWrapper<Assignment>().in(Assignment::getCourseId, courseIds));
                List<Long> assignmentIds = assignments.stream().map(Assignment::getId).collect(java.util.stream.Collectors.toList());
                if (!assignmentIds.isEmpty()) {
                    count += submitMapper.selectCount(new LambdaQueryWrapper<AssignmentSubmit>()
                            .in(AssignmentSubmit::getAssignmentId, assignmentIds)
                            .ge(AssignmentSubmit::getSubmitTime, start)
                            .lt(AssignmentSubmit::getSubmitTime, end));
                }
                List<Exam> exams = examMapper.selectList(new LambdaQueryWrapper<Exam>().in(Exam::getCourseId, courseIds));
                List<Long> examIds = exams.stream().map(Exam::getId).collect(java.util.stream.Collectors.toList());
                if (!examIds.isEmpty()) {
                    count += recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                            .in(ExamRecord::getExamId, examIds)
                            .ge(ExamRecord::getSubmitTime, start)
                            .lt(ExamRecord::getSubmitTime, end));
                }
            }
            DashboardVO.DailyActive da = new DashboardVO.DailyActive();
            da.setDate(date.toString());
            da.setCount((int) count);
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
                            .le(Exam::getStartTime, LocalDateTime.now())
                            .ge(Exam::getEndTime, LocalDateTime.now()));
        }
        vo.setUpcomingExamCount(upcomingExams);

        // 学习趋势：按学生近 7 天作业提交和考试提交统计
        List<DashboardVO.DailyActive> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long count = submitMapper.selectCount(new LambdaQueryWrapper<AssignmentSubmit>()
                    .eq(AssignmentSubmit::getStudentId, studentId)
                    .ge(AssignmentSubmit::getSubmitTime, start)
                    .lt(AssignmentSubmit::getSubmitTime, end));
            count += recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getStudentId, studentId)
                    .ge(ExamRecord::getSubmitTime, start)
                    .lt(ExamRecord::getSubmitTime, end));
            DashboardVO.DailyActive da = new DashboardVO.DailyActive();
            da.setDate(date.toString());
            da.setCount((int) count);
            trend.add(da);
        }
        vo.setActiveTrend(trend);
        return vo;
    }

    public AdminDashboardVO getAdminDashboard() {
        AdminDashboardVO vo = new AdminDashboardVO();

        // 管理员角色ID
        SysRole adminRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ROLE_ADMIN"));
        Long adminRoleId = adminRole != null ? adminRole.getId() : null;

        // 用户统计（排除管理员）
        LambdaQueryWrapper<SysUser> nonAdminWrapper = new LambdaQueryWrapper<>();
        if (adminRoleId != null) {
            nonAdminWrapper.ne(SysUser::getRoleId, adminRoleId);
        }
        vo.setTotalUsers(sysUserMapper.selectCount(nonAdminWrapper));

        // 教师数
        SysRole teacherRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ROLE_TEACHER"));
        vo.setTeacherCount(teacherRole != null ? sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, teacherRole.getId())) : 0);

        // 学生数
        SysRole studentRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ROLE_STUDENT"));
        vo.setStudentCount(studentRole != null ? sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, studentRole.getId())) : 0);

        // 课程总数
        vo.setCourseCount(courseMapper.selectCount(null));

        // 本月新增用户
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LambdaQueryWrapper<SysUser> newWrapper = new LambdaQueryWrapper<SysUser>()
                .ge(SysUser::getCreateTime, monthStart);
        if (adminRoleId != null) {
            newWrapper.ne(SysUser::getRoleId, adminRoleId);
        }
        vo.setNewUsersThisMonth(sysUserMapper.selectCount(newWrapper));

        // 今日活跃操作
        vo.setTodayActiveOps(operationLogMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                .apply("DATE(create_time) = CURDATE()")));

        // 近7日活跃趋势
        List<DashboardVO.DailyActive> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long count = operationLogMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                    .ge(OperationLog::getCreateTime, start)
                    .lt(OperationLog::getCreateTime, end));
            DashboardVO.DailyActive da = new DashboardVO.DailyActive();
            da.setDate(date.toString());
            da.setCount((int) count);
            trend.add(da);
        }
        vo.setActiveTrend(trend);

        // 最近10条操作记录
        List<OperationLog> logs = operationLogMapper.selectPage(
                new Page<>(1, 10),
                new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getCreateTime)
        ).getRecords();
        List<Map<String, Object>> recentOps = new ArrayList<>();
        for (OperationLog log : logs) {
            Map<String, Object> item = new HashMap<>();
            item.put("operatorName", log.getOperatorName());
            item.put("module", log.getModule());
            item.put("action", log.getAction());
            item.put("targetName", log.getTargetName());
            item.put("createTime", log.getCreateTime());
            recentOps.add(item);
        }
        vo.setRecentOperations(recentOps);

        return vo;
    }
}
