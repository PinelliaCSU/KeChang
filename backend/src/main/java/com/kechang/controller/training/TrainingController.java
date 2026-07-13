package com.kechang.controller.training;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.entity.TrainingMaterial;
import com.kechang.entity.TrainingProject;
import com.kechang.entity.TrainingReport;
import com.kechang.entity.TrainingTask;
import com.kechang.service.TrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trainings")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    // ==================== 项目管理 ====================

    @GetMapping
    public Result<PageResult<TrainingProject>> listProjects(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = PageResult.clampSize(size);
        return Result.success(trainingService.listProjects(page, size, uid(), hasRole("ROLE_TEACHER")));
    }

    @GetMapping("/{id}")
    public Result<TrainingProject> getProject(@PathVariable Long id) {
        return Result.success(trainingService.getProjectDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> createProject(@RequestBody TrainingProject p) {
        trainingService.createProject(p, uid());
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> updateProject(@PathVariable Long id, @RequestBody TrainingProject p) {
        trainingService.updateProject(id, p, uid());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> deleteProject(@PathVariable Long id) {
        trainingService.deleteProject(id, uid());
        return Result.success();
    }

    // ==================== 实习计划 ====================

    @PutMapping("/{id}/plan")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> updatePlan(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        TrainingProject project = new TrainingProject();
        project.setDescription(body.get("description") != null ? body.get("description").toString() : null);
        List<String> urls = body.get("attachmentUrls") instanceof List
                ? ((List<?>) body.get("attachmentUrls")).stream().map(Object::toString).toList() : null;
        List<String> names = body.get("attachmentNames") instanceof List
                ? ((List<?>) body.get("attachmentNames")).stream().map(Object::toString).toList() : null;
        List<Long> sizes = body.get("attachmentSizes") instanceof List
                ? ((List<?>) body.get("attachmentSizes")).stream().map(o -> o instanceof Number ? ((Number) o).longValue() : Long.parseLong(o.toString())).toList() : null;
        trainingService.updatePlan(id, project, urls, names, sizes, uid());
        return Result.success();
    }

    @GetMapping("/{id}/plan-attachments")
    public Result<List<?>> getPlanAttachments(@PathVariable Long id) {
        return Result.success(trainingService.getPlanAttachments(id));
    }

    // ==================== 资料管理 ====================

    @GetMapping("/{id}/materials")
    public Result<PageResult<TrainingMaterial>> listMaterials(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = PageResult.clampSize(size);
        return Result.success(trainingService.listMaterials(id, page, size));
    }

    @PostMapping("/{id}/materials")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> uploadMaterial(@PathVariable Long id, @RequestBody TrainingMaterial material) {
        trainingService.uploadMaterial(id, material, uid());
        return Result.success();
    }

    @DeleteMapping("/materials/{materialId}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> deleteMaterial(@PathVariable Long materialId) {
        trainingService.deleteMaterial(materialId, uid());
        return Result.success();
    }

    // ==================== 任务管理 ====================

    @PostMapping("/{id}/tasks")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> createTask(@PathVariable Long id, @RequestBody TrainingTask t) {
        trainingService.createTask(id, t, uid());
        return Result.success();
    }

    @GetMapping("/{id}/tasks")
    public Result<PageResult<TrainingTask>> listTasks(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String taskType) {
        size = PageResult.clampSize(size);
        return Result.success(trainingService.listTasks(id, page, size, uid(), hasRole("ROLE_STUDENT"), taskType));
    }

    @PutMapping("/tasks/{taskId}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> updateTask(@PathVariable Long taskId, @RequestBody TrainingTask t) {
        trainingService.updateTask(taskId, t, uid());
        return Result.success();
    }

    @DeleteMapping("/tasks/{taskId}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> deleteTask(@PathVariable Long taskId) {
        trainingService.deleteTask(taskId, uid());
        return Result.success();
    }

    // ==================== 报告管理 ====================

    @PostMapping("/tasks/{taskId}/report")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Void> submitReport(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        String content = body.get("content") != null ? body.get("content").toString() : null;
        String attachmentUrl = body.get("attachmentUrl") != null ? body.get("attachmentUrl").toString() : null;
        List<String> attachmentUrls = body.get("attachmentUrls") instanceof List
                ? ((List<?>) body.get("attachmentUrls")).stream().map(Object::toString).toList() : null;
        List<String> attachmentNames = body.get("attachmentNames") instanceof List
                ? ((List<?>) body.get("attachmentNames")).stream().map(Object::toString).toList() : null;
        List<Long> attachmentSizes = body.get("attachmentSizes") instanceof List
                ? ((List<?>) body.get("attachmentSizes")).stream().map(o -> o instanceof Number ? ((Number) o).longValue() : Long.parseLong(o.toString())).toList() : null;
        trainingService.submitReport(taskId, uid(), content, attachmentUrl, attachmentUrls, attachmentNames, attachmentSizes);
        return Result.success();
    }

    @GetMapping("/tasks/{taskId}/reports")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<PageResult<TrainingReport>> listReports(@PathVariable Long taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = PageResult.clampSize(size);
        return Result.success(trainingService.listReports(taskId, page, size, uid()));
    }

    @PutMapping("/reports/{reportId}/grade")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> gradeReport(@PathVariable Long reportId,
            @RequestParam Double score,
            @RequestParam(required = false) String comment) {
        trainingService.gradeReport(reportId, score, comment, uid());
        return Result.success();
    }
}
