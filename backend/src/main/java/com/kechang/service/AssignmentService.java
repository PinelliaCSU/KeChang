package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.dto.AssignmentDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 作业管理服务
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final AssignmentSubmitMapper submitMapper;
    private final AssignmentGradeMapper gradeMapper;

    public PageResult<Assignment> list(int page, int size, Long courseId) {
        LambdaQueryWrapper<Assignment> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(Assignment::getCourseId, courseId);
        }
        w.orderByDesc(Assignment::getCreateTime);
        Page<Assignment> p = assignmentMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void create(AssignmentDTO dto, Long teacherId) {
        Assignment a = new Assignment();
        a.setCourseId(dto.getCourseId());
        a.setTitle(dto.getTitle());
        a.setContent(dto.getContent());
        a.setAttachmentUrl(dto.getAttachmentUrl());
        a.setDeadline(dto.getDeadline());
        a.setTotalScore(dto.getTotalScore() != null ? dto.getTotalScore() : 100.0);
        a.setStatus("进行中");
        a.setCreateBy(teacherId);
        assignmentMapper.insert(a);
    }

    public Assignment getDetail(Long id) {
        return assignmentMapper.selectById(id);
    }

    public void update(Long id, AssignmentDTO dto) {
        Assignment a = assignmentMapper.selectById(id);
        if (a == null) {
            throw BusinessException.notFound("作业不存在");
        }
        a.setTitle(dto.getTitle());
        a.setContent(dto.getContent());
        a.setDeadline(dto.getDeadline());
        assignmentMapper.updateById(a);
    }

    public void delete(Long id) {
        assignmentMapper.deleteById(id);
    }

    @Transactional
    public void submit(Long assignmentId, Long studentId, String content, String attachmentUrl) {
        Assignment a = assignmentMapper.selectById(assignmentId);
        if (a == null) {
            throw BusinessException.notFound("作业不存在");
        }
        if (a.getDeadline() != null && LocalDateTime.now().isAfter(a.getDeadline())) {
            throw BusinessException.badRequest("已超过提交截止时间");
        }
        AssignmentSubmit exist = submitMapper.selectOne(new LambdaQueryWrapper<AssignmentSubmit>()
                .eq(AssignmentSubmit::getAssignmentId, assignmentId)
                .eq(AssignmentSubmit::getStudentId, studentId));
        if (exist != null) {
            throw BusinessException.badRequest("已提交过该作业");
        }

        AssignmentSubmit s = new AssignmentSubmit();
        s.setAssignmentId(assignmentId);
        s.setStudentId(studentId);
        s.setContent(content);
        s.setAttachmentUrl(attachmentUrl);
        s.setSubmitTime(LocalDateTime.now());
        s.setStatus("待批改");
        submitMapper.insert(s);
    }

    public PageResult<AssignmentSubmit> getSubmitList(Long assignmentId, int page, int size) {
        Page<AssignmentSubmit> p = submitMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AssignmentSubmit>().eq(AssignmentSubmit::getAssignmentId, assignmentId));
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void grade(Long submitId, Double score, String comment, Long teacherId) {
        AssignmentSubmit s = submitMapper.selectById(submitId);
        if (s == null) {
            throw BusinessException.notFound("提交记录不存在");
        }
        s.setScore(score);
        s.setStatus("已批改");
        submitMapper.updateById(s);

        AssignmentGrade g = gradeMapper.selectOne(new LambdaQueryWrapper<AssignmentGrade>()
                .eq(AssignmentGrade::getSubmitId, submitId));
        if (g == null) {
            g = new AssignmentGrade();
            g.setSubmitId(submitId);
        }
        g.setScore(score);
        g.setComment(comment);
        g.setGradedBy(teacherId);
        g.setGradedTime(LocalDateTime.now());
        gradeMapper.insertOrUpdate(g);
    }
}
