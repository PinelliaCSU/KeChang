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
    private final StudentProfileMapper studentProfileMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final GradeService gradeService;

    public PageResult<AssignmentVO> list(int page, int size, Long courseId) {
        LambdaQueryWrapper<Assignment> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(Assignment::getCourseId, courseId);
        }
        w.orderByDesc(Assignment::getCreateTime);
        Page<Assignment> p = assignmentMapper.selectPage(new Page<>(page, size), w);
        List<AssignmentVO> vos = p.getRecords().stream().map(a -> {
            AssignmentVO vo = new AssignmentVO();
            copyFields(a, vo);
            // 课程名称
            Course course = courseMapper.selectById(a.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getCourseName());
            }
            // 提交统计
            long submitCount = submitMapper.selectCount(
                    new LambdaQueryWrapper<AssignmentSubmit>().eq(AssignmentSubmit::getAssignmentId, a.getId()));
            vo.setSubmitCount((int) submitCount);
            long totalStudents = courseStudentMapper.selectCount(
                    new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getCourseId, a.getCourseId()));
            vo.setTotalStudents((int) totalStudents);
            return vo;
        }).collect(Collectors.toList());
        return PageResult.of(vos, p.getTotal(), page, size);
    }

    /**
     * 获取某个学生的作业列表（只显示其已选课程的作业）
     */
    public PageResult<AssignmentVO> listForStudent(Long studentId, int page, int size, Long courseId) {
        // 获取学生已选课程
        List<CourseStudent> enrollments = courseStudentMapper.selectList(
                new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getStudentId, studentId));
        List<Long> courseIds = enrollments.stream().map(CourseStudent::getCourseId).collect(Collectors.toList());

        LambdaQueryWrapper<Assignment> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(Assignment::getCourseId, courseId);
        }
        if (!courseIds.isEmpty()) {
            w.in(Assignment::getCourseId, courseIds);
        } else {
            // 未选任何课程则返回空
            return PageResult.of(List.of(), 0, page, size);
        }
        w.orderByDesc(Assignment::getCreateTime);
        Page<Assignment> p = assignmentMapper.selectPage(new Page<>(page, size), w);
        List<AssignmentVO> vos = p.getRecords().stream().map(a -> {
            AssignmentVO vo = new AssignmentVO();
            copyFields(a, vo);
            Course course = courseMapper.selectById(a.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getCourseName());
            }
            // 查询当前学生的提交状态
            AssignmentSubmit mySubmit = submitMapper.selectOne(
                    new LambdaQueryWrapper<AssignmentSubmit>()
                            .eq(AssignmentSubmit::getAssignmentId, a.getId())
                            .eq(AssignmentSubmit::getStudentId, studentId));
            if (mySubmit != null) {
                vo.setMySubmitStatus(mySubmit.getStatus());
                vo.setMyScore(mySubmit.getScore());
            }
            return vo;
        }).collect(Collectors.toList());
        return PageResult.of(vos, p.getTotal(), page, size);
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

    public AssignmentVO getDetail(Long id) {
        Assignment a = assignmentMapper.selectById(id);
        if (a == null) {
            return null;
        }
        AssignmentVO vo = new AssignmentVO();
        copyFields(a, vo);
        Course course = courseMapper.selectById(a.getCourseId());
        if (course != null) {
            vo.setCourseName(course.getCourseName());
        }
        return vo;
    }

    public void update(Long id, AssignmentDTO dto) {
        Assignment a = assignmentMapper.selectById(id);
        if (a == null) {
            throw BusinessException.notFound("作业不存在");
        }
        a.setTitle(dto.getTitle());
        a.setContent(dto.getContent());
        a.setDeadline(dto.getDeadline());
        a.setTotalScore(dto.getTotalScore());
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

    public PageResult<AssignmentSubmitVO> getSubmitList(Long assignmentId, int page, int size) {
        Page<AssignmentSubmit> p = submitMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AssignmentSubmit>().eq(AssignmentSubmit::getAssignmentId, assignmentId)
                        .orderByDesc(AssignmentSubmit::getSubmitTime));
        List<AssignmentSubmitVO> vos = p.getRecords().stream().map(s -> {
            AssignmentSubmitVO vo = new AssignmentSubmitVO();
            vo.setId(s.getId());
            vo.setAssignmentId(s.getAssignmentId());
            vo.setStudentId(s.getStudentId());
            vo.setContent(s.getContent());
            vo.setAttachmentUrl(s.getAttachmentUrl());
            vo.setSubmitTime(s.getSubmitTime());
            vo.setScore(s.getScore());
            vo.setStatus(s.getStatus());
            vo.setCreateTime(s.getCreateTime());
            // 学生信息
            StudentProfile sp = studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, s.getStudentId()));
            if (sp != null) {
                vo.setStudentName(sp.getRealName());
                vo.setStudentNo(sp.getStudentNo());
            }
            return vo;
        }).collect(Collectors.toList());
        return PageResult.of(vos, p.getTotal(), page, size);
    }

    @Transactional
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

        // 自动重新计算课程成绩：获取该作业的课程和学生，计算作业均分
        Assignment assignment = assignmentMapper.selectById(s.getAssignmentId());
        if (assignment != null) {
            // 计算该学生在该课程所有作业的均分
            List<AssignmentSubmit> allSubmits = submitMapper.selectList(
                    new LambdaQueryWrapper<AssignmentSubmit>()
                            .eq(AssignmentSubmit::getStudentId, s.getStudentId()));
            // 筛选该课程的作业提交
            double totalScore = 0;
            int count = 0;
            for (AssignmentSubmit sub : allSubmits) {
                Assignment ass = assignmentMapper.selectById(sub.getAssignmentId());
                if (ass != null && ass.getCourseId().equals(assignment.getCourseId())
                        && sub.getScore() != null) {
                    totalScore += sub.getScore();
                    count++;
                }
            }
            if (count > 0) {
                double avg = totalScore / count;
                GradeCourse gc = gradeService.getOrCreateGradeCourse(assignment.getCourseId(), s.getStudentId());
                gc.setAssignmentAvgScore(Math.round(avg * 10.0) / 10.0);
                gradeService.updateGradeCourse(gc);
                // 重新计算总评
                gradeService.recalculateGrade(assignment.getCourseId(), s.getStudentId());
            }
        }
    }

    private void copyFields(Assignment a, AssignmentVO vo) {
        vo.setId(a.getId());
        vo.setCourseId(a.getCourseId());
        vo.setTitle(a.getTitle());
        vo.setContent(a.getContent());
        vo.setAttachmentUrl(a.getAttachmentUrl());
        vo.setDeadline(a.getDeadline());
        vo.setTotalScore(a.getTotalScore());
        vo.setStatus(a.getStatus());
        vo.setCreateBy(a.getCreateBy());
        vo.setCreateTime(a.getCreateTime());
    }
}
