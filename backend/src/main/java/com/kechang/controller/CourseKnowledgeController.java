package com.kechang.controller;

import com.kechang.common.Result;
import com.kechang.dto.CourseKnowledgeAskDTO;
import com.kechang.dto.CourseKnowledgeManualDTO;
import com.kechang.entity.CourseKnowledgeDocument;
import com.kechang.service.CourseKnowledgeService;
import com.kechang.vo.CourseKnowledgeAskVO;
import com.kechang.vo.CourseKnowledgeStatsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course-knowledge")
@RequiredArgsConstructor
public class CourseKnowledgeController {

    private final CourseKnowledgeService courseKnowledgeService;

    @GetMapping("/courses")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER','ROLE_ADMIN')")
    public Result<List<CourseKnowledgeStatsVO>> courses() {
        return Result.success(courseKnowledgeService.listAvailableCourses(uid(), role()));
    }

    @GetMapping("/courses/{courseId}/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER','ROLE_ADMIN')")
    public Result<CourseKnowledgeStatsVO> stats(@PathVariable Long courseId) {
        return Result.success(courseKnowledgeService.getStats(courseId, uid(), role()));
    }

    @GetMapping("/courses/{courseId}/documents")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER','ROLE_ADMIN')")
    public Result<List<CourseKnowledgeDocument>> documents(@PathVariable Long courseId) {
        return Result.success(courseKnowledgeService.listDocuments(courseId, uid(), role()));
    }

    @PostMapping("/courses/{courseId}/rebuild")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Map<String, Object>> rebuild(@PathVariable Long courseId) {
        return Result.success(courseKnowledgeService.rebuildCourse(courseId, uid()));
    }

    @PostMapping("/documents")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    public Result<CourseKnowledgeDocument> addManual(@Valid @RequestBody CourseKnowledgeManualDTO dto) {
        return Result.success(courseKnowledgeService.addManual(dto, uid(), role()));
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    public Result<Void> deleteDocument(@PathVariable Long id) {
        courseKnowledgeService.deleteDocument(id, uid(), role());
        return Result.success();
    }

    @PostMapping("/ask")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER','ROLE_ADMIN')")
    public Result<CourseKnowledgeAskVO> ask(@Valid @RequestBody CourseKnowledgeAskDTO dto) {
        return Result.success(courseKnowledgeService.ask(dto, uid(), role()));
    }

    private Long uid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof Long id ? id : null;
    }

    private String role() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "";
        }
        return authentication.getAuthorities().stream()
                .map(Object::toString)
                .filter(item -> item.startsWith("ROLE_"))
                .findFirst()
                .orElse("");
    }
}
