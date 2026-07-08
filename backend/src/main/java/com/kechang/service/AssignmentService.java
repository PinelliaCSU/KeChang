package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.dto.AssignmentDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.vo.AssignmentSubmitVO;
import com.kechang.vo.AssignmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 作业管理服务
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final AssignmentSubmitMapper submitMapper;
    private final AssignmentGradeMapper gradeMapper;
    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final GradeService gradeService;

    public PageResult<AssignmentVO> list(int page, int size, Long courseId, Long chapterId, Long userId, String role) {
        LambdaQueryWrapper<Assignment> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(Assignment::getCourseId, courseId);
        }
        if (chapterId != null) {
            w.eq(Assignment::getChapterId, chapterId);
        }
        if ("ROLE_TEACHER".equals(role)) {
            w.eq(Assignment::getCreateBy, userId);
        }
        if ("ROLE_STUDENT".equals(role)) {
            List<Long> courseIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                            .eq(CourseStudent::getStudentId, userId))
                    .stream()
                    .map(CourseStudent::getCourseId)
                    .collect(Collectors.toList());
            List<Long> submittedAssignmentIds = submitMapper.selectList(new LambdaQueryWrapper<AssignmentSubmit>()
                            .eq(AssignmentSubmit::getStudentId, userId))
                    .stream()
                    .map(AssignmentSubmit::getAssignmentId)
                    .collect(Collectors.toList());
            if (courseIds.isEmpty() && submittedAssignmentIds.isEmpty()) {
                return PageResult.of(new ArrayList<>(), 0, page, size);
            }
            w.and(q -> {
                if (!courseIds.isEmpty()) {
                    q.in(Assignment::getCourseId, courseIds);
                }
                if (!submittedAssignmentIds.isEmpty()) {
                    if (!courseIds.isEmpty()) {
                        q.or();
                    }
                    q.in(Assignment::getId, submittedAssignmentIds);
                }
            });
        }
        w.orderByDesc(Assignment::getCreateTime);
        Page<Assignment> p = assignmentMapper.selectPage(new Page<>(page, size), w);
        Long studentId = "ROLE_STUDENT".equals(role) ? userId : null;
        List<AssignmentVO> records = p.getRecords().stream()
                .map(assignment -> toAssignmentVO(assignment, studentId))
                .collect(Collectors.toList());
        return PageResult.of(records, p.getTotal(), page, size);
    }

    public void create(AssignmentDTO dto, Long teacherId) {
        Assignment a = new Assignment();
        a.setCourseId(dto.getCourseId());
        a.setChapterId(dto.getChapterId());
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
        a.setChapterId(dto.getChapterId());
        a.setDeadline(dto.getDeadline());
        a.setAttachmentUrl(dto.getAttachmentUrl());
        a.setTotalScore(dto.getTotalScore() != null ? dto.getTotalScore() : a.getTotalScore());
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
        CourseStudent enrollment = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, a.getCourseId())
                .eq(CourseStudent::getStudentId, studentId));
        if (enrollment == null) {
            throw BusinessException.forbidden("未选该课程，不能提交该课程作业");
        }

        AssignmentSubmit exist = submitMapper.selectOne(new LambdaQueryWrapper<AssignmentSubmit>()
                .eq(AssignmentSubmit::getAssignmentId, assignmentId)
                .eq(AssignmentSubmit::getStudentId, studentId)
                .orderByDesc(AssignmentSubmit::getSubmitTime)
                .last("limit 1"));
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

    public PageResult<AssignmentSubmitVO> getSubmitList(Long assignmentId, int page, int size) {
        Page<AssignmentSubmit> p = submitMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AssignmentSubmit>()
                        .eq(AssignmentSubmit::getAssignmentId, assignmentId)
                        .orderByDesc(AssignmentSubmit::getSubmitTime));
        List<AssignmentSubmitVO> records = p.getRecords().stream()
                .map(this::toSubmitVO)
                .collect(Collectors.toList());
        return PageResult.of(records, p.getTotal(), page, size);
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
        if (g.getId() == null) {
            gradeMapper.insert(g);
        } else {
            gradeMapper.updateById(g);
        }
        Assignment assignment = assignmentMapper.selectById(s.getAssignmentId());
        if (assignment != null) {
            gradeService.recalculateGrade(assignment.getCourseId(), s.getStudentId());
        }
    }

    private AssignmentVO toAssignmentVO(Assignment assignment, Long studentId) {
        AssignmentVO vo = new AssignmentVO();
        vo.setId(assignment.getId());
        vo.setCourseId(assignment.getCourseId());
        vo.setChapterId(assignment.getChapterId());
        vo.setTitle(assignment.getTitle());
        vo.setContent(assignment.getContent());
        vo.setAttachmentUrl(assignment.getAttachmentUrl());
        vo.setDeadline(assignment.getDeadline());
        vo.setTotalScore(assignment.getTotalScore());
        vo.setStatus(assignment.getStatus());
        vo.setCreateBy(assignment.getCreateBy());
        vo.setCreateTime(assignment.getCreateTime());

        Course course = courseMapper.selectById(assignment.getCourseId());
        if (course != null) {
            vo.setCourseName(course.getCourseName());
        }

        Long submitCount = submitMapper.selectCount(new LambdaQueryWrapper<AssignmentSubmit>()
                .eq(AssignmentSubmit::getAssignmentId, assignment.getId()));
        Long enrolledCount = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, assignment.getCourseId()));
        vo.setSubmitCount(Math.toIntExact(submitCount));
        vo.setTotalStudents(Math.toIntExact(Math.max(enrolledCount, submitCount)));
        fillMySubmission(vo, assignment.getId(), studentId);
        return vo;
    }

    private void fillMySubmission(AssignmentVO vo, Long assignmentId, Long studentId) {
        if (studentId == null) {
            return;
        }
        AssignmentSubmit submit = submitMapper.selectOne(new LambdaQueryWrapper<AssignmentSubmit>()
                .eq(AssignmentSubmit::getAssignmentId, assignmentId)
                .eq(AssignmentSubmit::getStudentId, studentId)
                .orderByDesc(AssignmentSubmit::getSubmitTime)
                .last("limit 1"));
        vo.setSubmitted(submit != null);
        if (submit == null) {
            return;
        }
        vo.setMySubmitId(submit.getId());
        vo.setSubmitContent(submit.getContent());
        vo.setSubmitAttachmentUrl(submit.getAttachmentUrl());
        vo.setSubmitTime(submit.getSubmitTime());
        vo.setMySubmitStatus(submit.getStatus());
        vo.setMyScore(submit.getScore());

        AssignmentGrade grade = gradeMapper.selectOne(new LambdaQueryWrapper<AssignmentGrade>()
                .eq(AssignmentGrade::getSubmitId, submit.getId())
                .last("limit 1"));
        if (grade != null) {
            vo.setMyScore(grade.getScore());
            vo.setGradeComment(grade.getComment());
            vo.setGradedTime(grade.getGradedTime());
        }
    }

    private AssignmentSubmitVO toSubmitVO(AssignmentSubmit submit) {
        AssignmentSubmitVO vo = new AssignmentSubmitVO();
        vo.setId(submit.getId());
        vo.setAssignmentId(submit.getAssignmentId());
        vo.setStudentId(submit.getStudentId());
        vo.setContent(submit.getContent());
        vo.setAttachmentUrl(submit.getAttachmentUrl());
        vo.setSubmitTime(submit.getSubmitTime());
        vo.setScore(submit.getScore());
        vo.setStatus(submit.getStatus());
        vo.setCreateTime(submit.getCreateTime());

        AssignmentGrade grade = gradeMapper.selectOne(new LambdaQueryWrapper<AssignmentGrade>()
                .eq(AssignmentGrade::getSubmitId, submit.getId())
                .last("limit 1"));
        if (grade != null) {
            vo.setScore(grade.getScore());
            vo.setComment(grade.getComment());
            vo.setGradedTime(grade.getGradedTime());
        }

        StudentProfile profile = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, submit.getStudentId())
                .last("limit 1"));
        if (profile != null) {
            vo.setStudentName(profile.getRealName());
            vo.setStudentNo(profile.getStudentNo());
        }
        if (vo.getStudentName() == null) {
            SysUser user = sysUserMapper.selectById(submit.getStudentId());
            if (user != null) {
                vo.setStudentName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        }
        return vo;
    }
}
