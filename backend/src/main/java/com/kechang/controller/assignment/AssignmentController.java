package com.kechang.controller.assignment;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.dto.AssignmentDTO;
import com.kechang.entity.Assignment;
import com.kechang.service.AssignmentService;
import com.kechang.vo.AssignmentSubmitVO;
import com.kechang.vo.AssignmentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    private Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public Result<PageResult<AssignmentVO>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("");
        return Result.success(assignmentService.list(page, size, courseId, userId, role));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> create(@Valid @RequestBody AssignmentDTO dto) {
        assignmentService.create(dto, getUserId());
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Assignment> detail(@PathVariable Long id) {
        return Result.success(assignmentService.getDetail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AssignmentDTO dto) {
        assignmentService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Void> submit(@PathVariable Long id, @RequestParam(required = false) String content,
            @RequestParam(required = false) String attachmentUrl) {
        assignmentService.submit(id, getUserId(), content, attachmentUrl);
        return Result.success();
    }

    @GetMapping("/{id}/submits")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<PageResult<AssignmentSubmitVO>> submits(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(assignmentService.getSubmitList(id, page, size));
    }

    @PutMapping("/submits/{submitId}/grade")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> grade(@PathVariable Long submitId, @RequestParam Double score,
            @RequestParam(required = false) String comment) {
        assignmentService.grade(submitId, score, comment, getUserId());
        return Result.success();
    }
}
