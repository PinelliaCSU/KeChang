package com.kechang.controller;

import com.kechang.common.Result;
import com.kechang.service.DashboardService;
import com.kechang.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    private Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/teacher/stats")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<DashboardVO> teacherStats() {
        return Result.success(dashboardService.getTeacherDashboard(getUserId()));
    }

    @GetMapping("/student/stats")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public Result<DashboardVO> studentStats() {
        return Result.success(dashboardService.getStudentDashboard(getUserId()));
    }
}
