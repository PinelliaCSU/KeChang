package com.kechang.controller.exam;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.dto.ClassQuizAnswerDTO;
import com.kechang.dto.ClassQuizCreateDTO;
import com.kechang.service.ClassQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/class-quizzes")
public class ClassQuizController {

    private final ClassQuizService classQuizService;

    private Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .findFirst()
                .orElse(null);
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<PageResult<Map<String, Object>>> teacherList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId) {
        size = PageResult.clampSize(size);
        return Result.success(classQuizService.teacherList(getUserId(), courseId, page, size));
    }

    @GetMapping("/student/active")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<List<Map<String, Object>>> studentActiveList() {
        return Result.success(classQuizService.studentActiveList(getUserId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Map<String, Object>> create(@RequestBody ClassQuizCreateDTO dto) {
        return Result.success(classQuizService.create(dto, getUserId()));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(classQuizService.detail(id, getUserId(), currentRole()));
    }

    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> stats(@PathVariable Long id) {
        return Result.success(classQuizService.stats(id, getUserId(), currentRole()));
    }

    @PostMapping("/{id}/answer")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Map<String, Object>> submit(@PathVariable Long id, @RequestBody ClassQuizAnswerDTO dto) {
        return Result.success(classQuizService.submit(id, dto, getUserId()));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> close(@PathVariable Long id) {
        classQuizService.close(id, getUserId());
        return Result.success();
    }
}
