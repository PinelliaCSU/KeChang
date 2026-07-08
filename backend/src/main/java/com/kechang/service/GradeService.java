package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kechang.common.BusinessException;
import com.kechang.entity.Assignment;
import com.kechang.entity.AssignmentGrade;
import com.kechang.entity.AssignmentSubmit;
import com.kechang.entity.Course;
import com.kechang.entity.CourseStudent;
import com.kechang.entity.Exam;
import com.kechang.entity.ExamPaper;
import com.kechang.entity.ExamRecord;
import com.kechang.entity.GradeCourse;
import com.kechang.entity.StudentProfile;
import com.kechang.entity.SysUser;
import com.kechang.mapper.AssignmentGradeMapper;
import com.kechang.mapper.AssignmentMapper;
import com.kechang.mapper.AssignmentSubmitMapper;
import com.kechang.mapper.CourseMapper;
import com.kechang.mapper.CourseStudentMapper;
import com.kechang.mapper.ExamMapper;
import com.kechang.mapper.ExamPaperMapper;
import com.kechang.mapper.ExamRecordMapper;
import com.kechang.mapper.GradeCourseMapper;
import com.kechang.mapper.StudentProfileMapper;
import com.kechang.mapper.SysUserMapper;
import com.kechang.vo.GradeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成绩管理服务。
 *
 * 作业和考试是两套独立体系：没有发布、没有提交、没有批改的数据不写成 0 分，
 * 而是保持为 null。只有真实存在的成绩才参与课程总评和统计。
 */
@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeCourseMapper gradeCourseMapper;
    private final CourseMapper courseMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final AssignmentMapper assignmentMapper;
    private final AssignmentSubmitMapper assignmentSubmitMapper;
    private final AssignmentGradeMapper assignmentGradeMapper;
    private final ExamMapper examMapper;
    private final ExamPaperMapper examPaperMapper;
    private final ExamRecordMapper examRecordMapper;

    public List<GradeVO> getStudentGrades(Long studentId) {
        refreshStudentGrades(studentId);
        return buildGradeVOs(studentId);
    }

    public List<GradeVO> getMyGrades(Long studentId) {
        return getStudentGrades(studentId);
    }

    public List<GradeVO> getCourseGrades(Long courseId) {
        refreshCourseGrades(courseId);
        List<GradeCourse> grades = gradeCourseMapper.selectList(
                new LambdaQueryWrapper<GradeCourse>()
                        .eq(GradeCourse::getCourseId, courseId)
                        .orderByDesc(GradeCourse::getTotalScore));
        return grades.stream()
                .filter(this::hasAnyGrade)
                .map(this::toGradeVO)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getCourseStats(Long courseId) {
        List<GradeVO> grades = getCourseGrades(courseId);
        List<Double> scores = grades.stream()
                .map(GradeVO::getTotalScore)
                .filter(score -> score != null)
                .collect(Collectors.toList());
        long passed = scores.stream().filter(score -> score >= 60).count();

        Map<String, Object> result = new HashMap<>();
        result.put("avg", average(scores));
        result.put("max", max(scores));
        result.put("min", min(scores));
        result.put("distribution", buildDistribution(scores));
        result.put("total", grades.size());
        result.put("scoredTotal", scores.size());
        result.put("passed", passed);
        result.put("passRate", scores.isEmpty() ? null : Math.round(passed * 100.0 / scores.size()));
        return result;
    }

    public Map<String, Object> getPlatformOverview() {
        courseMapper.selectList(new LambdaQueryWrapper<>())
                .forEach(course -> refreshCourseGrades(course.getId()));
        List<GradeCourse> grades = gradeCourseMapper.selectList(new LambdaQueryWrapper<>())
                .stream()
                .filter(this::hasAnyGrade)
                .collect(Collectors.toList());
        List<Double> scores = grades.stream()
                .map(GradeCourse::getTotalScore)
                .filter(score -> score != null)
                .collect(Collectors.toList());
        long courseCount = courseMapper.selectCount(new LambdaQueryWrapper<>());
        long enrollmentCount = courseStudentMapper.selectCount(new LambdaQueryWrapper<>());
        long passCount = scores.stream().filter(score -> score >= 60).count();

        Map<String, Object> result = new HashMap<>();
        result.put("courseCount", courseCount);
        result.put("enrollmentCount", enrollmentCount);
        result.put("gradeCount", grades.size());
        result.put("scoredTotal", scores.size());
        result.put("avg", average(scores));
        result.put("max", max(scores));
        result.put("min", min(scores));
        result.put("passRate", scores.isEmpty() ? null : Math.round(passCount * 100.0 / scores.size()));
        result.put("distribution", buildDistribution(scores));
        return result;
    }

    public List<GradeVO> exportGrades(Long courseId) {
        return getCourseGrades(courseId);
    }

    @Transactional
    public void refreshCourseGrades(Long courseId) {
        List<CourseStudent> enrollments = courseStudentMapper.selectList(
                new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getCourseId, courseId));
        for (CourseStudent enrollment : enrollments) {
            recalculateGrade(courseId, enrollment.getStudentId());
        }
    }

    @Transactional
    public void refreshStudentGrades(Long studentId) {
        List<CourseStudent> enrollments = courseStudentMapper.selectList(
                new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getStudentId, studentId));
        for (CourseStudent enrollment : enrollments) {
            recalculateGrade(enrollment.getCourseId(), studentId);
        }
    }

    @Transactional
    public void recalculateGrade(Long courseId, Long studentId) {
        GradeCourse gc = getOrCreateGradeCourse(courseId, studentId);
        Double examAvg = calculateExamAverage(courseId, studentId);
        Double assignmentAvg = calculateAssignmentAverage(courseId, studentId);
        Double total = calculateTotal(examAvg, assignmentAvg);

        gc.setExamAvgScore(round1(examAvg));
        gc.setAssignmentAvgScore(round1(assignmentAvg));
        gc.setTotalScore(round1(total));
        gc.setGradeLevel(total == null ? null : levelOf(total));
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
            gc.setExamAvgScore(null);
            gc.setAssignmentAvgScore(null);
            gc.setTotalScore(null);
            gc.setGradeLevel(null);
            gradeCourseMapper.insert(gc);
        }
        return gc;
    }

    public void updateGradeCourse(GradeCourse gc) {
        gradeCourseMapper.updateById(gc);
    }

    public void checkCourseOwner(Long courseId, Long teacherId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw BusinessException.forbidden("只能查看自己课程的成绩");
        }
    }

    private List<GradeVO> buildGradeVOs(Long studentId) {
        List<GradeCourse> grades = gradeCourseMapper.selectList(
                new LambdaQueryWrapper<GradeCourse>().eq(GradeCourse::getStudentId, studentId));
        return grades.stream()
                .filter(this::hasAnyGrade)
                .map(this::toGradeVO)
                .collect(Collectors.toList());
    }

    private GradeVO toGradeVO(GradeCourse g) {
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
                new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, g.getStudentId()));
        if (sp != null) {
            vo.setStudentName(StringUtils.hasText(sp.getRealName()) ? sp.getRealName() : fallbackName(g.getStudentId()));
            vo.setStudentNo(sp.getStudentNo());
        } else {
            vo.setStudentName(fallbackName(g.getStudentId()));
            vo.setStudentNo(String.valueOf(g.getStudentId()));
        }
        return vo;
    }

    private Double calculateAssignmentAverage(Long courseId, Long studentId) {
        List<Assignment> assignments = assignmentMapper.selectList(
                new LambdaQueryWrapper<Assignment>().eq(Assignment::getCourseId, courseId));
        List<Double> scores = new ArrayList<>();
        for (Assignment assignment : assignments) {
            AssignmentSubmit submit = assignmentSubmitMapper.selectOne(new LambdaQueryWrapper<AssignmentSubmit>()
                    .eq(AssignmentSubmit::getAssignmentId, assignment.getId())
                    .eq(AssignmentSubmit::getStudentId, studentId));
            if (submit == null) {
                continue;
            }
            AssignmentGrade grade = assignmentGradeMapper.selectOne(new LambdaQueryWrapper<AssignmentGrade>()
                    .eq(AssignmentGrade::getSubmitId, submit.getId()));
            Double score = grade != null ? grade.getScore() : submit.getScore();
            if (score != null) {
                double full = assignment.getTotalScore() != null && assignment.getTotalScore() > 0
                        ? assignment.getTotalScore() : 100.0;
                scores.add(score * 100.0 / full);
            }
        }
        return average(scores);
    }

    private Double calculateExamAverage(Long courseId, Long studentId) {
        List<Exam> exams = examMapper.selectList(new LambdaQueryWrapper<Exam>().eq(Exam::getCourseId, courseId));
        List<Double> scores = new ArrayList<>();
        for (Exam exam : exams) {
            ExamRecord record = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getExamId, exam.getId())
                    .eq(ExamRecord::getStudentId, studentId)
                    .eq(ExamRecord::getIsSubmitted, 1));
            if (record == null || record.getTotalScore() == null) {
                continue;
            }
            ExamPaper paper = examPaperMapper.selectById(exam.getPaperId());
            double full = paper != null && paper.getTotalScore() != null && paper.getTotalScore() > 0
                    ? paper.getTotalScore() : 100.0;
            scores.add(record.getTotalScore() * 100.0 / full);
        }
        return average(scores);
    }

    private Double calculateTotal(Double examAvg, Double assignmentAvg) {
        if (examAvg == null && assignmentAvg == null) {
            return null;
        }
        if (examAvg == null) {
            return assignmentAvg;
        }
        if (assignmentAvg == null) {
            return examAvg;
        }
        return examAvg * 0.6 + assignmentAvg * 0.4;
    }

    private boolean hasAnyGrade(GradeCourse grade) {
        return grade.getExamAvgScore() != null
                || grade.getAssignmentAvgScore() != null
                || grade.getTotalScore() != null;
    }

    private Map<String, Long> buildDistribution(List<Double> scores) {
        Map<String, Long> dist = new LinkedHashMap<>();
        if (scores.isEmpty()) {
            return dist;
        }
        dist.put("优秀(90+)", scores.stream().filter(score -> score >= 90).count());
        dist.put("良好(80-89)", scores.stream().filter(score -> score >= 80 && score < 90).count());
        dist.put("中等(70-79)", scores.stream().filter(score -> score >= 70 && score < 80).count());
        dist.put("及格(60-69)", scores.stream().filter(score -> score >= 60 && score < 70).count());
        dist.put("不及格(<60)", scores.stream().filter(score -> score < 60).count());
        return dist;
    }

    private String levelOf(double total) {
        if (total >= 90) return "优秀";
        if (total >= 80) return "良好";
        if (total >= 70) return "中等";
        if (total >= 60) return "及格";
        return "不及格";
    }

    private Double average(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        return round1(values.stream().mapToDouble(Double::doubleValue).average().getAsDouble());
    }

    private Double max(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        return round1(values.stream().mapToDouble(Double::doubleValue).max().getAsDouble());
    }

    private Double min(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        return round1(values.stream().mapToDouble(Double::doubleValue).min().getAsDouble());
    }

    private Double round1(Double value) {
        return value == null ? null : Math.round(value * 10.0) / 10.0;
    }

    private String fallbackName(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return "--";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}
