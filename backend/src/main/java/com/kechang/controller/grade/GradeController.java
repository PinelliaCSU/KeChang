package com.kechang.controller.grade;

import com.kechang.common.Result;
import com.kechang.service.GradeService;
import com.kechang.vo.GradeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    private Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<List<GradeVO>> studentGrades(@PathVariable Long studentId) {
        return Result.success(gradeService.getStudentGrades(studentId));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<List<GradeVO>> myGrades() {
        return Result.success(gradeService.getMyGrades(getUserId()));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<List<GradeVO>> courseGrades(@PathVariable Long courseId) {
        gradeService.checkCourseOwner(courseId, getUserId());
        return Result.success(gradeService.getCourseGrades(courseId));
    }

    @GetMapping("/course/{courseId}/stats")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Map<String, Object>> courseStats(@PathVariable Long courseId) {
        gradeService.checkCourseOwner(courseId, getUserId());
        return Result.success(gradeService.getCourseStats(courseId));
    }

    @GetMapping("/course/{courseId}/export")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<List<GradeVO>> exportGrades(@PathVariable Long courseId) {
        gradeService.checkCourseOwner(courseId, getUserId());
        return Result.success(gradeService.exportGrades(courseId));
    }

    @GetMapping("/platform/overview")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<Map<String, Object>> platformOverview() {
        return Result.success(gradeService.getPlatformOverview());
    }
}
