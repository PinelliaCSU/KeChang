package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kechang.common.PageResult;
import com.kechang.entity.GradeCourse;
import com.kechang.entity.Course;
import com.kechang.entity.StudentProfile;
import com.kechang.mapper.GradeCourseMapper;
import com.kechang.mapper.CourseMapper;
import com.kechang.mapper.StudentProfileMapper;
import com.kechang.vo.GradeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 成绩管理服务
 */
@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeCourseMapper gradeCourseMapper;
    private final CourseMapper courseMapper;
    private final StudentProfileMapper studentProfileMapper;

    public List<GradeVO> getStudentGrades(Long studentId) {
        return buildGradeVOs(studentId);
    }

    public List<GradeVO> getMyGrades(Long studentId) {
        return buildGradeVOs(studentId);
    }

    public List<GradeVO> getCourseGrades(Long courseId) {
        List<GradeCourse> grades = gradeCourseMapper.selectList(
                new LambdaQueryWrapper<GradeCourse>().eq(GradeCourse::getCourseId, courseId));
        return grades.stream().map(g -> {
            GradeVO vo = new GradeVO();
            vo.setCourseId(g.getCourseId());
            vo.setStudentId(g.getStudentId());
            vo.setExamAvgScore(g.getExamAvgScore());
            vo.setAssignmentAvgScore(g.getAssignmentAvgScore());
            vo.setTotalScore(g.getTotalScore());
            vo.setGradeLevel(g.getGradeLevel());
            // 查询学生信息
            StudentProfile sp = studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, g.getStudentId()));
            if (sp != null) {
                vo.setStudentName(sp.getRealName());
                vo.setStudentNo(sp.getStudentNo());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getCourseStats(Long courseId) {
        List<GradeCourse> grades = gradeCourseMapper.selectList(
                new LambdaQueryWrapper<GradeCourse>().eq(GradeCourse::getCourseId, courseId));
        double avg = grades.stream().mapToDouble(g -> g.getTotalScore() != null ? g.getTotalScore() : 0).average().orElse(0);
        double max = grades.stream().mapToDouble(g -> g.getTotalScore() != null ? g.getTotalScore() : 0).max().orElse(0);
        double min = grades.stream().mapToDouble(g -> g.getTotalScore() != null ? g.getTotalScore() : 0).min().orElse(0);

        Map<String, Long> dist = new LinkedHashMap<>();
        dist.put("优秀(90+)", grades.stream().filter(g -> g.getTotalScore() != null && g.getTotalScore() >= 90).count());
        dist.put("良好(80-89)", grades.stream().filter(g -> g.getTotalScore() != null && g.getTotalScore() >= 80 && g.getTotalScore() < 90).count());
        dist.put("中等(70-79)", grades.stream().filter(g -> g.getTotalScore() != null && g.getTotalScore() >= 70 && g.getTotalScore() < 80).count());
        dist.put("及格(60-69)", grades.stream().filter(g -> g.getTotalScore() != null && g.getTotalScore() >= 60 && g.getTotalScore() < 70).count());
        dist.put("不及格(<60)", grades.stream().filter(g -> g.getTotalScore() != null && g.getTotalScore() < 60).count());

        Map<String, Object> result = new HashMap<>();
        result.put("avg", String.format("%.1f", avg));
        result.put("max", max);
        result.put("min", min);
        result.put("distribution", dist);
        result.put("total", grades.size());
        return result;
    }

    public List<GradeVO> exportGrades(Long courseId) {
        return getCourseGrades(courseId);
    }

    private List<GradeVO> buildGradeVOs(Long studentId) {
        List<GradeCourse> grades = gradeCourseMapper.selectList(
                new LambdaQueryWrapper<GradeCourse>().eq(GradeCourse::getStudentId, studentId));
        return grades.stream().map(g -> {
            GradeVO vo = new GradeVO();
            vo.setCourseId(g.getCourseId());
            vo.setStudentId(g.getStudentId());
            Course course = courseMapper.selectById(g.getCourseId());
            vo.setCourseName(course != null ? course.getCourseName() : "未知课程");
            vo.setExamAvgScore(g.getExamAvgScore());
            vo.setAssignmentAvgScore(g.getAssignmentAvgScore());
            vo.setTotalScore(g.getTotalScore());
            vo.setGradeLevel(g.getGradeLevel());
            StudentProfile sp = studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, studentId));
            if (sp != null) {
                vo.setStudentName(sp.getRealName());
                vo.setStudentNo(sp.getStudentNo());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 根据作业均分和考试均分重新计算总评成绩
     */
    @Transactional
    public void recalculateGrade(Long courseId, Long studentId) {
        GradeCourse gc = getOrCreateGradeCourse(courseId, studentId);
        double exam = gc.getExamAvgScore() != null ? gc.getExamAvgScore() : 0;
        double assign = gc.getAssignmentAvgScore() != null ? gc.getAssignmentAvgScore() : 0;
        // 综合成绩 = 考试均分*0.6 + 作业均分*0.4
        double total = exam * 0.6 + assign * 0.4;
        gc.setTotalScore(Math.round(total * 10.0) / 10.0);
        // 等级判断
        if (total >= 90) {
            gc.setGradeLevel("优秀"); 
        }else if (total >= 80) {
            gc.setGradeLevel("良好"); 
        }else if (total >= 70) {
            gc.setGradeLevel("中等"); 
        }else if (total >= 60) {
            gc.setGradeLevel("及格"); 
        }else {
            gc.setGradeLevel("不及格");
        }
        gradeCourseMapper.updateById(gc);
    }

    @Transactional
    public GradeCourse getOrCreateGradeCourse(Long courseId, Long studentId) {
        GradeCourse gc = gradeCourseMapper.selectOne(
                new LambdaQueryWrapper<GradeCourse>()
                        .eq(GradeCourse::getCourseId, courseId)
                        .eq(GradeCourse::getStudentId, studentId));
        if (gc == null) {
            gc = new GradeCourse();
            gc.setCourseId(courseId);
            gc.setStudentId(studentId);
            gc.setExamAvgScore(0.0);
            gc.setAssignmentAvgScore(0.0);
            gc.setTotalScore(0.0);
            gc.setGradeLevel("不及格");
            gradeCourseMapper.insert(gc);
        }
        return gc;
    }

    public void updateGradeCourse(GradeCourse gc) {
        gradeCourseMapper.updateById(gc);
    }
}
