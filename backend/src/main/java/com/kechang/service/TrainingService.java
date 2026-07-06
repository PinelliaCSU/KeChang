package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingProjectMapper projectMapper;
    private final TrainingTaskMapper taskMapper;
    private final TrainingReportMapper reportMapper;

    // Projects
    public PageResult<TrainingProject> listProjects(int page, int size, Long courseId) {
        LambdaQueryWrapper<TrainingProject> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(TrainingProject::getCourseId, courseId);
        }
        w.orderByDesc(TrainingProject::getCreateTime);
        Page<TrainingProject> p = projectMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void createProject(TrainingProject project) {
        projectMapper.insert(project);
    }

    public void updateProject(Long id, TrainingProject project) {
        project.setId(id);
        projectMapper.updateById(project);
    }

    // Tasks
    public PageResult<TrainingTask> listTasks(Long projectId, int page, int size) {
        Page<TrainingTask> p = taskMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<TrainingTask>().eq(TrainingTask::getProjectId, projectId)
                        .orderByAsc(TrainingTask::getSortOrder));
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void createTask(TrainingTask task) {
        taskMapper.insert(task);
    }

    // Reports
    public void submitReport(Long taskId, Long studentId, String content, String attachmentUrl) {
        TrainingReport r = reportMapper.selectOne(new LambdaQueryWrapper<TrainingReport>()
                .eq(TrainingReport::getTaskId, taskId).eq(TrainingReport::getStudentId, studentId));
        if (r != null) {
            throw BusinessException.badRequest("已提交过该任务报告");
        }
        r = new TrainingReport();
        r.setTaskId(taskId);
        r.setStudentId(studentId);
        r.setReportContent(content);
        r.setAttachmentUrl(attachmentUrl);
        r.setSubmitTime(LocalDateTime.now());
        r.setStatus("待批改");
        reportMapper.insert(r);
    }

    public PageResult<TrainingReport> listReports(Long taskId, int page, int size) {
        Page<TrainingReport> p = reportMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<TrainingReport>().eq(TrainingReport::getTaskId, taskId));
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void gradeReport(Long reportId, Double score, String comment) {
        TrainingReport r = reportMapper.selectById(reportId);
        if (r == null) {
            throw BusinessException.notFound("报告不存在");
        }
        r.setScore(score);
        r.setComment(comment);
        r.setStatus("已批改");
        reportMapper.updateById(r);
    }
}
