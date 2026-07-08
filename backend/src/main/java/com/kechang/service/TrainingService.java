package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.entity.StudentProfile;
import com.kechang.entity.SysUser;
import com.kechang.entity.TrainingProject;
import com.kechang.entity.TrainingReport;
import com.kechang.entity.TrainingTask;
import com.kechang.mapper.StudentProfileMapper;
import com.kechang.mapper.SysUserMapper;
import com.kechang.mapper.TrainingProjectMapper;
import com.kechang.mapper.TrainingReportMapper;
import com.kechang.mapper.TrainingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingProjectMapper projectMapper;
    private final TrainingTaskMapper taskMapper;
    private final TrainingReportMapper reportMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final SysUserMapper sysUserMapper;

    public PageResult<TrainingProject> listProjects(int page, int size, Long userId, boolean teacher) {
        LambdaQueryWrapper<TrainingProject> wrapper = new LambdaQueryWrapper<>();
        if (teacher) {
            wrapper.eq(TrainingProject::getCreateBy, userId);
        }
        wrapper.orderByDesc(TrainingProject::getCreateTime);
        Page<TrainingProject> result = projectMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::fillProjectExtra);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public void createProject(TrainingProject project, Long teacherId) {
        if (!StringUtils.hasText(project.getProjectName())) {
            throw BusinessException.badRequest("请填写实训项目名称");
        }
        project.setCreateBy(teacherId);
        project.setCourseId(null);
        if (!StringUtils.hasText(project.getStatus())) {
            project.setStatus("进行中");
        }
        projectMapper.insert(project);
    }

    @Transactional
    public void updateProject(Long id, TrainingProject project, Long teacherId) {
        TrainingProject old = checkProjectOwner(id, teacherId);
        project.setId(id);
        project.setCreateBy(old.getCreateBy());
        project.setCourseId(null);
        projectMapper.updateById(project);
    }

    @Transactional
    public void deleteProject(Long id, Long teacherId) {
        checkProjectOwner(id, teacherId);
        List<TrainingTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<TrainingTask>()
                .eq(TrainingTask::getProjectId, id));
        for (TrainingTask task : tasks) {
            reportMapper.delete(new LambdaQueryWrapper<TrainingReport>().eq(TrainingReport::getTaskId, task.getId()));
            taskMapper.deleteById(task.getId());
        }
        projectMapper.deleteById(id);
    }

    public PageResult<TrainingTask> listTasks(Long projectId, int page, int size, Long userId, boolean student) {
        requireProject(projectId);
        Page<TrainingTask> result = taskMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<TrainingTask>()
                        .eq(TrainingTask::getProjectId, projectId)
                        .orderByAsc(TrainingTask::getSortOrder)
                        .orderByAsc(TrainingTask::getCreateTime));
        result.getRecords().forEach(task -> fillTaskExtra(task, userId, student));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public void createTask(Long projectId, TrainingTask task, Long teacherId) {
        checkProjectOwner(projectId, teacherId);
        if (!StringUtils.hasText(task.getTaskName())) {
            throw BusinessException.badRequest("请填写任务名称");
        }
        task.setProjectId(projectId);
        if (task.getSortOrder() == null) {
            task.setSortOrder(0);
        }
        taskMapper.insert(task);
    }

    @Transactional
    public void deleteTask(Long taskId, Long teacherId) {
        TrainingTask task = requireTask(taskId);
        checkProjectOwner(task.getProjectId(), teacherId);
        reportMapper.delete(new LambdaQueryWrapper<TrainingReport>().eq(TrainingReport::getTaskId, taskId));
        taskMapper.deleteById(taskId);
    }

    @Transactional
    public void submitReport(Long taskId, Long studentId, String content, String attachmentUrl) {
        requireTask(taskId);
        if (!StringUtils.hasText(content) && !StringUtils.hasText(attachmentUrl)) {
            throw BusinessException.badRequest("请填写报告内容或附件地址");
        }
        TrainingReport report = reportMapper.selectOne(new LambdaQueryWrapper<TrainingReport>()
                .eq(TrainingReport::getTaskId, taskId)
                .eq(TrainingReport::getStudentId, studentId));
        if (report == null) {
            report = new TrainingReport();
            report.setTaskId(taskId);
            report.setStudentId(studentId);
            report.setStatus("待批改");
            report.setSubmitTime(LocalDateTime.now());
            report.setReportContent(content);
            report.setAttachmentUrl(attachmentUrl);
            reportMapper.insert(report);
            return;
        }
        if ("已批改".equals(report.getStatus())) {
            throw BusinessException.badRequest("报告已批改，不能再次修改");
        }
        report.setReportContent(content);
        report.setAttachmentUrl(attachmentUrl);
        report.setSubmitTime(LocalDateTime.now());
        report.setStatus("待批改");
        reportMapper.updateById(report);
    }

    public PageResult<TrainingReport> listReports(Long taskId, int page, int size, Long teacherId) {
        TrainingTask task = requireTask(taskId);
        checkProjectOwner(task.getProjectId(), teacherId);
        Page<TrainingReport> result = reportMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<TrainingReport>()
                        .eq(TrainingReport::getTaskId, taskId)
                        .orderByDesc(TrainingReport::getSubmitTime));
        result.getRecords().forEach(this::fillReportStudent);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Transactional
    public void gradeReport(Long reportId, Double score, String comment, Long teacherId) {
        TrainingReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw BusinessException.notFound("报告不存在");
        }
        TrainingTask task = requireTask(report.getTaskId());
        checkProjectOwner(task.getProjectId(), teacherId);
        if (score == null || score < 0 || score > 100) {
            throw BusinessException.badRequest("评分必须在 0 到 100 之间");
        }
        report.setScore(score);
        report.setComment(comment);
        report.setStatus("已批改");
        reportMapper.updateById(report);
    }

    private void fillProjectExtra(TrainingProject project) {
        project.setCourseName(null);
        Long taskCount = taskMapper.selectCount(new LambdaQueryWrapper<TrainingTask>()
                .eq(TrainingTask::getProjectId, project.getId()));
        project.setTaskCount(taskCount);
        if (taskCount == 0) {
            project.setReportCount(0L);
            return;
        }
        List<Long> taskIds = taskMapper.selectList(new LambdaQueryWrapper<TrainingTask>()
                        .eq(TrainingTask::getProjectId, project.getId()))
                .stream()
                .map(TrainingTask::getId)
                .collect(Collectors.toList());
        project.setReportCount(taskIds.isEmpty() ? 0L : reportMapper.selectCount(new LambdaQueryWrapper<TrainingReport>()
                .in(TrainingReport::getTaskId, taskIds)));
    }

    private void fillTaskExtra(TrainingTask task, Long userId, boolean student) {
        task.setReportCount(reportMapper.selectCount(new LambdaQueryWrapper<TrainingReport>()
                .eq(TrainingReport::getTaskId, task.getId())));
        if (!student) {
            return;
        }
        TrainingReport report = reportMapper.selectOne(new LambdaQueryWrapper<TrainingReport>()
                .eq(TrainingReport::getTaskId, task.getId())
                .eq(TrainingReport::getStudentId, userId));
        if (report == null) {
            task.setReportStatus("未提交");
            return;
        }
        task.setMyReportId(report.getId());
        task.setReportContent(report.getReportContent());
        task.setReportAttachmentUrl(report.getAttachmentUrl());
        task.setSubmitTime(report.getSubmitTime());
        task.setScore(report.getScore());
        task.setComment(report.getComment());
        task.setReportStatus(report.getStatus());
    }

    private void fillReportStudent(TrainingReport report) {
        StudentProfile profile = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, report.getStudentId()));
        if (profile != null) {
            report.setStudentNo(profile.getStudentNo());
            report.setStudentName(StringUtils.hasText(profile.getRealName()) ? profile.getRealName() : fallbackName(report.getStudentId()));
            return;
        }
        report.setStudentNo(String.valueOf(report.getStudentId()));
        report.setStudentName(fallbackName(report.getStudentId()));
    }

    private TrainingProject requireProject(Long projectId) {
        TrainingProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.notFound("实训项目不存在");
        }
        return project;
    }

    private TrainingTask requireTask(Long taskId) {
        TrainingTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.notFound("实训任务不存在");
        }
        return task;
    }

    private TrainingProject checkProjectOwner(Long projectId, Long teacherId) {
        TrainingProject project = requireProject(projectId);
        if (!teacherId.equals(project.getCreateBy())) {
            throw BusinessException.forbidden("只能管理自己创建的实训项目");
        }
        return project;
    }

    private String fallbackName(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return "--";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}
