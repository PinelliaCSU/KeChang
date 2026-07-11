package com.kechang.controller.course;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.dto.ChapterDTO;
import com.kechang.dto.CourseDTO;
import com.kechang.entity.CourseResource;
import com.kechang.entity.StudentProfile;
import com.kechang.service.BailianApiService;
import com.kechang.service.CourseService;
import com.kechang.service.FileProcessingService;
import com.kechang.vo.CourseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 课程管理控制器
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final BailianApiService bailianApiService;
    private final FileProcessingService fileProcessingService;

    /**
     * 课程列表
     */
    @GetMapping
    public Result<PageResult<CourseVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") Boolean onlyEnrolled) {
        return Result.success(courseService.getCourseList(page, size, keyword, status, currentUserId(), currentRole(), onlyEnrolled));
    }

    /**
     * 课程详情
     */
    @GetMapping("/{id}")
    public Result<CourseVO> detail(@PathVariable Long id) {
        return Result.success(courseService.getCourseDetail(id, currentUserId(), currentRole()));
    }

    /**
     * 创建课程
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> create(@Valid @RequestBody CourseDTO dto) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        courseService.createCourse(dto, userId);
        return Result.success();
    }

    /**
     * 更新课程
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CourseDTO dto) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        courseService.updateCourse(id, dto, userId);
        return Result.success();
    }

    /**
     * 删除课程
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        courseService.deleteCourse(id, userId);
        return Result.success();
    }

    /**
     * 添加章节
     */
    @PostMapping("/{id}/chapters")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> addChapter(@PathVariable Long id, @Valid @RequestBody ChapterDTO dto) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        courseService.addChapter(id, dto, userId);
        return Result.success();
    }

    /**
     * 更新章节
     */
    @PutMapping("/{id}/chapters/{chapterId}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> updateChapter(@PathVariable Long id, @PathVariable Long chapterId,
            @Valid @RequestBody ChapterDTO dto) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        courseService.updateChapter(id, chapterId, dto, userId);
        return Result.success();
    }

    /**
     * 删除章节
     */
    @DeleteMapping("/{id}/chapters/{chapterId}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> deleteChapter(@PathVariable Long id, @PathVariable Long chapterId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        courseService.deleteChapter(id, chapterId, userId);
        return Result.success();
    }

    @Value("${kechang.upload.path:./uploads}")
    private String uploadBasePath;

    // ... (other fields via @RequiredArgsConstructor)
    /**
     * 上传课程资源
     */
    @PostMapping("/{id}/resources")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> uploadResource(@PathVariable Long id,
            @RequestParam(required = false) Long chapterId,
            @RequestPart("file") MultipartFile file) throws IOException {
        // 使用配置的路径 + courses/{id} 子目录
        Path baseDir = Paths.get(uploadBasePath).toAbsolutePath().normalize();
        Path courseDir = baseDir.resolve("courses").resolve(String.valueOf(id));
        Files.createDirectories(courseDir);

        String safeName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replace("\\", "/").replaceAll(".*/", "")
                : "file";
        String fileName = UUID.randomUUID() + "_" + safeName;
        Path destFile = courseDir.resolve(fileName);
        Files.copy(file.getInputStream(), destFile, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = "/uploads/courses/" + id + "/" + fileName;
        String resourceType = getResourceType(safeName);
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        courseService.uploadResource(id, chapterId, safeName, resourceType, fileUrl, Files.size(destFile), userId);
        return Result.success();
    }

    /**
     * 查看选课学生（教师和学生均可查看）
     */
    @GetMapping("/{id}/students")
    public Result<PageResult<StudentProfile>> students(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
        return Result.success(courseService.getStudents(id, page, size, currentUserId(), currentRole()));
    }

    /**
     * 学生选课
     */
    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Void> enroll(@PathVariable Long id) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        courseService.enrollCourse(id, userId);
        return Result.success();
    }

    /**
     * 学生退课
     */
    @DeleteMapping("/{id}/enroll")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Void> unenroll(@PathVariable Long id) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        courseService.unenrollCourse(id, userId);
        return Result.success();
    }

    /**
     * 教师踢出学生
     */
    @DeleteMapping("/{id}/students/{studentId}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        courseService.removeStudent(id, studentId, userId);
        return Result.success();
    }

    /**
     * 查看课程资源列表
     */
    @GetMapping("/{id}/resources")
    public Result<List<CourseResource>> listResources(@PathVariable Long id,
            @RequestParam(required = false) Long chapterId) {
        return Result.success(courseService.getResources(id, chapterId, currentUserId(), currentRole()));
    }

    @PostMapping("/{courseId}/resources/{resourceId}/view")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Void> markResourceViewed(@PathVariable Long courseId, @PathVariable Long resourceId) {
        courseService.markResourceViewed(courseId, resourceId, currentUserId());
        return Result.success();
    }

    /**
     * 删除课程资源
     */
    @DeleteMapping("/{courseId}/resources/{resourceId}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> deleteResource(@PathVariable Long courseId, @PathVariable Long resourceId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        courseService.deleteResource(courseId, resourceId, userId);
        return Result.success();
    }

    /**
     * AI 课件总结 — 对课程资源内容进行智能摘要
     */
    @PostMapping("/{courseId}/resources/{resourceId}/summarize")
    public Result<Map<String, String>> summarizeResource(@PathVariable Long courseId, @PathVariable Long resourceId) {
        // 获取课程信息（用于提供上下文）
        CourseVO course = courseService.getCourseDetail(courseId, currentUserId(), currentRole());

        // 获取资源信息
        List<CourseResource> resources = courseService.getResources(courseId, null, currentUserId(), currentRole());
        CourseResource target = resources.stream()
                .filter(r -> r.getId().equals(resourceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("资源不存在或无权访问"));

        // 从文件中提取文本（传入资源名，媒体文件会返回占位描述）
        String fileContent = fileProcessingService.extractText(target.getFileUrl(), target.getResourceName());
        if (fileContent != null && fileContent.startsWith("[ASR ")) {
            return Result.error(500, fileContent);
        }

        // 调用 AI 生成总结（传入课程名作为上下文）
        String summary = bailianApiService.summarizeResource(
                target.getResourceName(), fileContent, course.getCourseName());

        return Result.success(Map.of(
                "summary", summary,
                "resourceName", target.getResourceName()
        ));
    }

    private String getResourceType(String filename) {
        if (filename == null) {
            return "LINK";
        }
        String ext = filename.toLowerCase();
        if (ext.endsWith(".mp4") || ext.endsWith(".avi") || ext.endsWith(".mov")) {
            return "VIDEO";
        }
        if (ext.endsWith(".pdf")) {
            return "PDF";
        }
        if (ext.endsWith(".ppt") || ext.endsWith(".pptx")) {
            return "PPT";
        }
        return "LINK";
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return null;
        }
        return (Long) authentication.getPrincipal();
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
}
