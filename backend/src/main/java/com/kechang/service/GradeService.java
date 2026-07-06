package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kechang.common.PageResult;
import com.kechang.entity.GradeCourse;
import com.kechang.entity.Course;
import com.kechang.mapper.GradeCourseMapper;
import com.kechang.mapper.CourseMapper;
import com.kechang.vo.GradeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
            vo.setExamAvgScore(g.getExamAvgScore());
            vo.setAssignmentAvgScore(g.getAssignmentAvgScore());
            vo.setTotalScore(g.getTotalScore());
            vo.setGradeLevel(g.getGradeLevel());
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
            Course course = courseMapper.selectById(g.getCourseId());
            vo.setCourseName(course != null ? course.getCourseName() : "未知课程");
            vo.setExamAvgScore(g.getExamAvgScore());
            vo.setAssignmentAvgScore(g.getAssignmentAvgScore());
            vo.setTotalScore(g.getTotalScore());
            vo.setGradeLevel(g.getGradeLevel());
            return vo;
        }).collect(Collectors.toList());
    }
}
