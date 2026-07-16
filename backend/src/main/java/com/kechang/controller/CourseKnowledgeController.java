package com.kechang.controller;

import com.kechang.common.Result;
import com.kechang.dto.CourseKnowledgeAskDTO;
import com.kechang.dto.CourseKnowledgeManualDTO;
import com.kechang.dto.RebuildTaskInfo;
import com.kechang.entity.CourseKnowledgeDocument;
import com.kechang.entity.CourseKnowledgeQaHistory;
import com.kechang.service.CourseKnowledgeService;
import com.kechang.service.RebuildTaskManager;
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
    private final RebuildTaskManager rebuildTaskManager;

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

    /**
     * 启动异步重建知识库，立即返回 taskId，前端通过 /rebuild/{taskId}/status 轮询进度。
     */
    @PostMapping("/courses/{courseId}/rebuild")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Map<String, String>> rebuild(@PathVariable Long courseId) {
        courseKnowledgeService.checkTaggedAll(courseId, uid(), "ROLE_TEACHER");
        String taskId = rebuildTaskManager.create(courseId, uid());
        courseKnowledgeService.rebuildCourseAsync(courseId, uid(), taskId);
        return Result.success(Map.of("taskId", taskId));
    }

    /**
     * 查询重建任务状态。
     */
    @GetMapping("/rebuild/{taskId}/status")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<RebuildTaskInfo> rebuildStatus(@PathVariable String taskId) {
        RebuildTaskInfo info = rebuildTaskManager.get(taskId);
        if (info == null) {
            return Result.error(404, "任务不存在或已过期");
        }
        return Result.success(info);
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

    @GetMapping("/courses/{courseId}/history")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER','ROLE_ADMIN')")
    public Result<List<CourseKnowledgeQaHistory>> history(@PathVariable Long courseId) {
        return Result.success(courseKnowledgeService.listHistory(courseId, uid(), role()));
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
