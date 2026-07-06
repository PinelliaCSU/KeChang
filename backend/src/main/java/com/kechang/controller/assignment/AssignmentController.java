package com.kechang.controller.assignment;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.dto.AssignmentDTO;
import com.kechang.entity.Assignment;
import com.kechang.entity.AssignmentSubmit;
import com.kechang.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public Result<PageResult<Assignment>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId) {
        return Result.success(assignmentService.list(page, size, courseId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> create(@Valid @RequestBody AssignmentDTO dto) {
        assignmentService.create(dto, getUserId());
        return Result.success("作业布置成功");
    }

    @GetMapping("/{id}")
    public Result<Assignment> detail(@PathVariable Long id) {
        return Result.success(assignmentService.getDetail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AssignmentDTO dto) {
        assignmentService.update(id, dto);
        return Result.success("作业更新成功");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return Result.success("作业已删除");
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public Result<Void> submit(@PathVariable Long id, @RequestParam(required = false) String content,
            @RequestParam(required = false) String attachmentUrl) {
        assignmentService.submit(id, getUserId(), content, attachmentUrl);
        return Result.success("提交成功");
    }

    @GetMapping("/{id}/submits")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<PageResult<AssignmentSubmit>> submits(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(assignmentService.getSubmitList(id, page, size));
    }

    @PutMapping("/submits/{submitId}/grade")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> grade(@PathVariable Long submitId, @RequestParam Double score,
            @RequestParam(required = false) String comment) {
        assignmentService.grade(submitId, score, comment, getUserId());
        return Result.success("评分成功");
    }
}
