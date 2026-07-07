package com.kechang.controller.course;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.dto.ChapterDTO;
import com.kechang.dto.CourseDTO;
import com.kechang.entity.CourseResource;
import com.kechang.entity.StudentProfile;
import com.kechang.service.CourseService;
import com.kechang.vo.CourseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 课程管理控制器
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * 课程列表
     */
    @GetMapping
    public Result<PageResult<CourseVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(courseService.getCourseList(page, size, keyword, status));
    }

    /**
     * 课程详情
     */
    @GetMapping("/{id}")
    public Result<CourseVO> detail(@PathVariable Long id) {
        return Result.success(courseService.getCourseDetail(id));
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
        courseService.addChapter(id, dto);
        return Result.success();
    }

    /**
     * 更新章节
     */
    @PutMapping("/{id}/chapters/{chapterId}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> updateChapter(@PathVariable Long id, @PathVariable Long chapterId,
            @Valid @RequestBody ChapterDTO dto) {
        courseService.updateChapter(chapterId, dto);
        return Result.success();
    }

    /**
     * 删除章节
     */
    @DeleteMapping("/{id}/chapters/{chapterId}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> deleteChapter(@PathVariable Long id, @PathVariable Long chapterId) {
        courseService.deleteChapter(chapterId);
        return Result.success();
    }

    /**
     * 上传课程资源
     */
    @PostMapping("/{id}/resources")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> uploadResource(@PathVariable Long id,
            @RequestParam(required = false) Long chapterId,
            @RequestParam("file") MultipartFile file) throws IOException {
        // 简单本地文件存储
        String uploadDir = "./uploads/courses/" + id + "/";
        new File(uploadDir).mkdirs();
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File destFile = new File(uploadDir + fileName);
        file.transferTo(destFile);
        String fileUrl = "/uploads/courses/" + id + "/" + fileName;
        String resourceType = getResourceType(file.getOriginalFilename());
        courseService.uploadResource(id, chapterId, file.getOriginalFilename(), resourceType, fileUrl, file.getSize());
        return Result.success();
    }

    /**
     * 查看选课学生
     */
    @GetMapping("/{id}/students")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<PageResult<StudentProfile>> students(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(courseService.getStudents(id, page, size));
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
     * 查看课程资源列表
     */
    @GetMapping("/{id}/resources")
    public Result<List<CourseResource>> listResources(@PathVariable Long id,
            @RequestParam(required = false) Long chapterId) {
        return Result.success(courseService.getResources(id, chapterId));
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
}
