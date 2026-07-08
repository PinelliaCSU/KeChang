package com.kechang.controller.assignment;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.dto.AssignmentDTO;
import com.kechang.entity.Assignment;
import com.kechang.service.AssignmentService;
import com.kechang.vo.AssignmentSubmitVO;
import com.kechang.vo.AssignmentVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long chapterId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("");
        return Result.success(assignmentService.list(page, size, courseId, chapterId, userId, role));
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
    public Result<Void> submit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String content = body.get("content") != null ? body.get("content").toString() : null;
        String attachmentUrl = body.get("attachmentUrl") != null ? body.get("attachmentUrl").toString() : null;
        List<String> attachmentUrls = body.get("attachmentUrls") instanceof List
                ? ((List<?>) body.get("attachmentUrls")).stream().map(Object::toString).toList() : null;
        List<String> attachmentNames = body.get("attachmentNames") instanceof List
                ? ((List<?>) body.get("attachmentNames")).stream().map(Object::toString).toList() : null;
        List<Long> attachmentSizes = body.get("attachmentSizes") instanceof List
                ? ((List<?>) body.get("attachmentSizes")).stream().map(o -> o instanceof Number ? ((Number) o).longValue() : Long.parseLong(o.toString())).toList() : null;
        assignmentService.submit(id, getUserId(), content, attachmentUrl, attachmentUrls, attachmentNames, attachmentSizes);
        return Result.success();
    }

    @GetMapping("/{id}/submits")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<PageResult<AssignmentSubmitVO>> submits(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(assignmentService.getSubmitList(id, page, size));
    }

    @GetMapping("/{id}/submits/archive")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public void downloadSubmitArchive(@PathVariable Long id, HttpServletResponse response) throws IOException {
        String fileName = assignmentService.archiveFileName(id);
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        assignmentService.writeSubmitArchive(id, getUserId(), response.getOutputStream());
    }

    @PutMapping("/submits/{submitId}/grade")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> grade(@PathVariable Long submitId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Double score,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String feedbackMarks) {
        if (body != null) {
            if (body.get("score") instanceof Number number) {
                score = number.doubleValue();
            } else if (body.get("score") != null) {
                score = Double.parseDouble(body.get("score").toString());
            }
            if (body.get("comment") != null) {
                comment = body.get("comment").toString();
            }
            if (body.get("feedbackMarks") != null) {
                feedbackMarks = body.get("feedbackMarks").toString();
            }
        }
        assignmentService.grade(submitId, score, comment, feedbackMarks, getUserId());
        return Result.success();
    }
}
