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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final ClassQuizMapper classQuizMapper;
    private final ClassQuizAnswerMapper classQuizAnswerMapper;
    private final CheckinSessionMapper checkinSessionMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final TrainingProjectMapper trainingProjectMapper;
    private final TrainingTaskMapper trainingTaskMapper;
    private final TrainingReportMapper trainingReportMapper;
    private final StudentMaterialMapper studentMaterialMapper;

    public DashboardVO getTeacherDashboard(Long teacherId) {
        DashboardVO vo = new DashboardVO();
        List<Course> teacherCourses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getTeacherId, teacherId));
        List<Long> courseIds = teacherCourses.stream().map(Course::getId).collect(Collectors.toList());

        vo.setCourseCount((long) teacherCourses.size());

        List<Long> assignmentIds = assignmentIdsByCourseIds(courseIds);
        long pendingSubmits = assignmentIds.isEmpty() ? 0 : submitMapper.selectCount(
                new LambdaQueryWrapper<AssignmentSubmit>()
                        .in(AssignmentSubmit::getAssignmentId, assignmentIds)
                        .eq(AssignmentSubmit::getStatus, "待批改"));
        vo.setPendingAssignmentCount(pendingSubmits);

        vo.setPendingQuestionCount(courseIds.isEmpty() ? 0 : postMapper.selectCount(
                new LambdaQueryWrapper<QuestionPost>()
                        .in(QuestionPost::getCourseId, courseIds)
                        .eq(QuestionPost::getReplyCount, 0)));

        List<Long> examIds = examIdsByCourseIds(courseIds);
        List<Long> quizIds = quizIdsByCourseIds(courseIds);
        List<Long> checkinSessionIds = checkinSessionIdsByCourseIds(courseIds);
        List<Long> trainingTaskIds = trainingTaskIdsByTeacher(teacherId);

        vo.setActiveTrend(buildTrend((start, end) ->
                countAssignmentSubmits(assignmentIds, start, end)
                        + countExamSubmits(examIds, null, start, end)
                        + countQuizAnswers(quizIds, null, start, end)
                        + countCheckins(checkinSessionIds, null, start, end)
                        + countTrainingReports(trainingTaskIds, null, start, end)
                        + countQuestionPosts(courseIds, null, start, end)));
        return vo;
    }

    public DashboardVO getStudentDashboard(Long studentId) {
        DashboardVO vo = new DashboardVO();
        List<CourseStudent> enrollments = courseStudentMapper.selectList(
                new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getStudentId, studentId));
        List<Long> enrolledCourseIds = enrollments.stream().map(CourseStudent::getCourseId).collect(Collectors.toList());

        vo.setCourseCount((long) enrollments.size());
        vo.setPendingAssignmentCount(countPendingAssignments(enrolledCourseIds, studentId));
        vo.setUpcomingExamCount(countOngoingExams(enrolledCourseIds));

        List<Long> examIds = examIdsByCourseIds(enrolledCourseIds);
        List<Long> quizIds = quizIdsByCourseIds(enrolledCourseIds);
        List<Long> checkinSessionIds = checkinSessionIdsByCourseIds(enrolledCourseIds);

        vo.setActiveTrend(buildTrend((start, end) ->
                submitMapper.selectCount(new LambdaQueryWrapper<AssignmentSubmit>()
                        .eq(AssignmentSubmit::getStudentId, studentId)
                        .ge(AssignmentSubmit::getSubmitTime, start)
                        .lt(AssignmentSubmit::getSubmitTime, end))
                        + countExamSubmits(examIds, studentId, start, end)
                        + countQuizAnswers(quizIds, studentId, start, end)
                        + countCheckins(checkinSessionIds, studentId, start, end)
                        + countTrainingReports(List.of(), studentId, start, end)
                        + studentMaterialMapper.selectCount(new LambdaQueryWrapper<StudentMaterial>()
                        .eq(StudentMaterial::getStudentId, studentId)
                        .ge(StudentMaterial::getCreateTime, start)
                        .lt(StudentMaterial::getCreateTime, end))
                        + countQuestionPosts(enrolledCourseIds, studentId, start, end)));
        return vo;
    }

    public AdminDashboardVO getAdminDashboard() {
        AdminDashboardVO vo = new AdminDashboardVO();

        SysRole adminRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ROLE_ADMIN"));
        Long adminRoleId = adminRole != null ? adminRole.getId() : null;

        LambdaQueryWrapper<SysUser> nonAdminWrapper = new LambdaQueryWrapper<>();
        if (adminRoleId != null) {
            nonAdminWrapper.ne(SysUser::getRoleId, adminRoleId);
        }
        vo.setTotalUsers(sysUserMapper.selectCount(nonAdminWrapper));

        SysRole teacherRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ROLE_TEACHER"));
        vo.setTeacherCount(teacherRole != null ? sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, teacherRole.getId())) : 0);

        SysRole studentRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ROLE_STUDENT"));
        vo.setStudentCount(studentRole != null ? sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, studentRole.getId())) : 0);

        vo.setCourseCount(courseMapper.selectCount(null));

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LambdaQueryWrapper<SysUser> newWrapper = new LambdaQueryWrapper<SysUser>()
                .ge(SysUser::getCreateTime, monthStart);
        if (adminRoleId != null) {
            newWrapper.ne(SysUser::getRoleId, adminRoleId);
        }
        vo.setNewUsersThisMonth(sysUserMapper.selectCount(newWrapper));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
        vo.setTodayActiveOps(countPlatformActivity(todayStart, tomorrowStart));
        vo.setActiveTrend(buildTrend(this::countPlatformActivity));

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

    private List<DashboardVO.DailyActive> buildTrend(ActivityCounter counter) {
        List<DashboardVO.DailyActive> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            DashboardVO.DailyActive item = new DashboardVO.DailyActive();
            item.setDate(date.toString());
            item.setCount((int) counter.count(start, end));
            trend.add(item);
        }
        return trend;
    }

    private long countPendingAssignments(List<Long> courseIds, Long studentId) {
        if (courseIds.isEmpty()) {
            return 0;
        }
        List<Assignment> assignments = assignmentMapper.selectList(
                new LambdaQueryWrapper<Assignment>().in(Assignment::getCourseId, courseIds));
        long pending = 0;
        for (Assignment assignment : assignments) {
            Long submitted = submitMapper.selectCount(new LambdaQueryWrapper<AssignmentSubmit>()
                    .eq(AssignmentSubmit::getAssignmentId, assignment.getId())
                    .eq(AssignmentSubmit::getStudentId, studentId));
            if (submitted == null || submitted == 0) {
                pending++;
            }
        }
        return pending;
    }

    private long countOngoingExams(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        return examMapper.selectCount(new LambdaQueryWrapper<Exam>()
                .in(Exam::getCourseId, courseIds)
                .le(Exam::getStartTime, now)
                .ge(Exam::getEndTime, now));
    }

    private long countPlatformActivity(LocalDateTime start, LocalDateTime end) {
        return operationLogMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                .ge(OperationLog::getCreateTime, start)
                .lt(OperationLog::getCreateTime, end))
                + submitMapper.selectCount(new LambdaQueryWrapper<AssignmentSubmit>()
                .ge(AssignmentSubmit::getSubmitTime, start)
                .lt(AssignmentSubmit::getSubmitTime, end))
                + recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getIsSubmitted, 1)
                .ge(ExamRecord::getSubmitTime, start)
                .lt(ExamRecord::getSubmitTime, end))
                + classQuizAnswerMapper.selectCount(new LambdaQueryWrapper<ClassQuizAnswer>()
                .ge(ClassQuizAnswer::getSubmitTime, start)
                .lt(ClassQuizAnswer::getSubmitTime, end))
                + checkinRecordMapper.selectCount(new LambdaQueryWrapper<CheckinRecord>()
                .ge(CheckinRecord::getCheckinTime, start)
                .lt(CheckinRecord::getCheckinTime, end))
                + trainingReportMapper.selectCount(new LambdaQueryWrapper<TrainingReport>()
                .ge(TrainingReport::getSubmitTime, start)
                .lt(TrainingReport::getSubmitTime, end))
                + studentMaterialMapper.selectCount(new LambdaQueryWrapper<StudentMaterial>()
                .ge(StudentMaterial::getCreateTime, start)
                .lt(StudentMaterial::getCreateTime, end))
                + postMapper.selectCount(new LambdaQueryWrapper<QuestionPost>()
                .ge(QuestionPost::getCreateTime, start)
                .lt(QuestionPost::getCreateTime, end));
    }

    private List<Long> assignmentIdsByCourseIds(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>().in(Assignment::getCourseId, courseIds))
                .stream().map(Assignment::getId).collect(Collectors.toList());
    }

    private List<Long> examIdsByCourseIds(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return examMapper.selectList(new LambdaQueryWrapper<Exam>().in(Exam::getCourseId, courseIds))
                .stream().map(Exam::getId).collect(Collectors.toList());
    }

    private List<Long> quizIdsByCourseIds(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return classQuizMapper.selectList(new LambdaQueryWrapper<ClassQuiz>().in(ClassQuiz::getCourseId, courseIds))
                .stream().map(ClassQuiz::getId).collect(Collectors.toList());
    }

    private List<Long> checkinSessionIdsByCourseIds(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return checkinSessionMapper.selectList(new LambdaQueryWrapper<CheckinSession>().in(CheckinSession::getCourseId, courseIds))
                .stream().map(CheckinSession::getId).collect(Collectors.toList());
    }

    private List<Long> trainingTaskIdsByTeacher(Long teacherId) {
        List<Long> projectIds = trainingProjectMapper.selectList(
                        new LambdaQueryWrapper<TrainingProject>().eq(TrainingProject::getCreateBy, teacherId))
                .stream().map(TrainingProject::getId).collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            return List.of();
        }
        return trainingTaskMapper.selectList(new LambdaQueryWrapper<TrainingTask>().in(TrainingTask::getProjectId, projectIds))
                .stream().map(TrainingTask::getId).collect(Collectors.toList());
    }

    private long countAssignmentSubmits(List<Long> assignmentIds, LocalDateTime start, LocalDateTime end) {
        if (assignmentIds.isEmpty()) {
            return 0;
        }
        return submitMapper.selectCount(new LambdaQueryWrapper<AssignmentSubmit>()
                .in(AssignmentSubmit::getAssignmentId, assignmentIds)
                .ge(AssignmentSubmit::getSubmitTime, start)
                .lt(AssignmentSubmit::getSubmitTime, end));
    }

    private long countExamSubmits(List<Long> examIds, Long studentId, LocalDateTime start, LocalDateTime end) {
        if (examIds.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<ExamRecord>()
                .in(ExamRecord::getExamId, examIds)
                .eq(ExamRecord::getIsSubmitted, 1)
                .ge(ExamRecord::getSubmitTime, start)
                .lt(ExamRecord::getSubmitTime, end);
        if (studentId != null) {
            wrapper.eq(ExamRecord::getStudentId, studentId);
        }
        return recordMapper.selectCount(wrapper);
    }

    private long countQuizAnswers(List<Long> quizIds, Long studentId, LocalDateTime start, LocalDateTime end) {
        if (quizIds.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<ClassQuizAnswer> wrapper = new LambdaQueryWrapper<ClassQuizAnswer>()
                .in(ClassQuizAnswer::getQuizId, quizIds)
                .ge(ClassQuizAnswer::getSubmitTime, start)
                .lt(ClassQuizAnswer::getSubmitTime, end);
        if (studentId != null) {
            wrapper.eq(ClassQuizAnswer::getStudentId, studentId);
        }
        return classQuizAnswerMapper.selectCount(wrapper);
    }

    private long countCheckins(List<Long> sessionIds, Long studentId, LocalDateTime start, LocalDateTime end) {
        if (sessionIds.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<CheckinRecord> wrapper = new LambdaQueryWrapper<CheckinRecord>()
                .in(CheckinRecord::getSessionId, sessionIds)
                .ge(CheckinRecord::getCheckinTime, start)
                .lt(CheckinRecord::getCheckinTime, end);
        if (studentId != null) {
            wrapper.eq(CheckinRecord::getStudentId, studentId);
        }
        return checkinRecordMapper.selectCount(wrapper);
    }

    private long countTrainingReports(List<Long> taskIds, Long studentId, LocalDateTime start, LocalDateTime end) {
        if ((taskIds == null || taskIds.isEmpty()) && studentId == null) {
            return 0;
        }
        LambdaQueryWrapper<TrainingReport> wrapper = new LambdaQueryWrapper<TrainingReport>()
                .ge(TrainingReport::getSubmitTime, start)
                .lt(TrainingReport::getSubmitTime, end);
        if (taskIds != null && !taskIds.isEmpty()) {
            wrapper.in(TrainingReport::getTaskId, taskIds);
        }
        if (studentId != null) {
            wrapper.eq(TrainingReport::getStudentId, studentId);
        }
        return trainingReportMapper.selectCount(wrapper);
    }

    private long countQuestionPosts(List<Long> courseIds, Long studentId, LocalDateTime start, LocalDateTime end) {
        if ((courseIds == null || courseIds.isEmpty()) && studentId == null) {
            return 0;
        }
        LambdaQueryWrapper<QuestionPost> wrapper = new LambdaQueryWrapper<QuestionPost>()
                .ge(QuestionPost::getCreateTime, start)
                .lt(QuestionPost::getCreateTime, end);
        if (courseIds != null && !courseIds.isEmpty()) {
            wrapper.in(QuestionPost::getCourseId, courseIds);
        }
        if (studentId != null) {
            wrapper.eq(QuestionPost::getCreateBy, studentId);
        }
        return postMapper.selectCount(wrapper);
    }

    @FunctionalInterface
    private interface ActivityCounter {
        long count(LocalDateTime start, LocalDateTime end);
    }
}
