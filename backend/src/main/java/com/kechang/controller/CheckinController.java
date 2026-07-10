package com.kechang.controller;

import com.kechang.common.Result;
import com.kechang.dto.CheckinCreateDTO;
import com.kechang.dto.CheckinSubmitDTO;
import com.kechang.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课堂签到控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/checkin")
public class CheckinController {

    private final CheckinService checkinService;

    private Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElse(null);
    }

    // ==================== 教师端 ====================
    @GetMapping("/teacher")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<List<Map<String, Object>>> teacherList(@RequestParam(required = false) Long courseId) {
        return Result.success(checkinService.teacherList(getUserId(), courseId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Map<String, Object>> create(@RequestBody CheckinCreateDTO dto) {
        return Result.success(checkinService.create(dto, getUserId()));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(checkinService.detail(id, getUserId(), currentRole()));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Map<String, Object>> stats(@PathVariable Long id) {
        return Result.success(checkinService.stats(id, getUserId(), currentRole()));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> close(@PathVariable Long id) {
        checkinService.close(id, getUserId());
        return Result.success();
    }

    // ==================== 学生端 ====================
    @GetMapping("/student/active")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<List<Map<String, Object>>> studentActiveList() {
        return Result.success(checkinService.studentActiveList(getUserId()));
    }

    @PostMapping("/{id}/checkin")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Map<String, Object>> checkin(@PathVariable Long id, @RequestBody CheckinSubmitDTO dto) {
        return Result.success(checkinService.checkin(id, dto, getUserId()));
    }

    // ==================== 课程维度 ====================
    @GetMapping("/course/{courseId}/records")
    public Result<List<Map<String, Object>>> courseRecords(@PathVariable Long courseId) {
        return Result.success(checkinService.courseRecords(courseId, getUserId(), currentRole()));
    }

    @GetMapping("/course/{courseId}/stats")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Map<String, Object>> courseAttendanceStats(@PathVariable Long courseId) {
        return Result.success(checkinService.courseAttendanceStats(courseId, getUserId()));
    }
}
