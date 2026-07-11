package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.dto.ChapterDTO;
import com.kechang.dto.CourseDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.vo.ChapterVO;
import com.kechang.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程管理服务
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseResourceMapper resourceMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final SysUserMapper sysUserMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final OperationLogService operationLogService;

    /**
     * 分页查询课程
     */
    public PageResult<CourseVO> getCourseList(int page, int size, String keyword, String status) {
        return getCourseList(page, size, keyword, status, null, null);
    }

    public PageResult<CourseVO> getCourseList(int page, int size, String keyword, String status, Long currentUserId,
            String role) {
        return getCourseList(page, size, keyword, status, currentUserId, role, false);
    }

    public PageResult<CourseVO> getCourseList(int page, int size, String keyword, String status, Long currentUserId,
            String role, Boolean onlyEnrolled) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Course::getCourseName, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Course::getStatus, status);
        }
        if ("ROLE_TEACHER".equals(role) && currentUserId != null) {
            wrapper.eq(Course::getTeacherId, currentUserId);
        }
        if ("ROLE_STUDENT".equals(role) && Boolean.TRUE.equals(onlyEnrolled) && currentUserId != null) {
            List<Long> courseIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getStudentId, currentUserId))
                    .stream().map(CourseStudent::getCourseId).collect(Collectors.toList());
            if (courseIds.isEmpty()) {
                return PageResult.of(new ArrayList<>(), 0, page, size);
            }
            wrapper.in(Course::getId, courseIds);
        }
        wrapper.orderByDesc(Course::getCreateTime);

        Page<Course> pageResult = courseMapper.selectPage(new Page<>(page, size), wrapper);
        Long currentStudentId = "ROLE_STUDENT".equals(role) ? currentUserId : null;
        List<CourseVO> records = pageResult.getRecords().stream().map(c -> {
            CourseVO vo = new CourseVO();
            copyCourseFields(c, vo);
            SysUser teacher = sysUserMapper.selectById(c.getTeacherId());
            if (teacher != null) {
                vo.setTeacherName(teacher.getNickname());
            }
            vo.setEnrolled(isEnrolled(c.getId(), currentStudentId));
            if (currentStudentId != null) {
                vo.setProgress(computeTimeProgress(c));
            }
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(records, pageResult.getTotal(), page, size);
    }

    /**
     * 课程详情
     */
    public CourseVO getCourseDetail(Long courseId) {
        return getCourseDetail(courseId, null, null);
    }

    public CourseVO getCourseDetail(Long courseId, Long currentUserId, String role) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        checkCourseReadable(course, currentUserId, role);
        CourseVO vo = new CourseVO();
        copyCourseFields(course, vo);
        SysUser teacher = sysUserMapper.selectById(course.getTeacherId());
        if (teacher != null) {
            vo.setTeacherName(teacher.getNickname());
        }
        // 章节列表
        List<CourseChapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<CourseChapter>().eq(CourseChapter::getCourseId, courseId)
                        .orderByAsc(CourseChapter::getSortOrder));
        List<ChapterVO> chapterVOs = new ArrayList<>();
        for (CourseChapter ch : chapters) {
            ChapterVO cv = new ChapterVO();
            cv.setId(ch.getId());
            cv.setChapterName(ch.getChapterName());
            cv.setSortOrder(ch.getSortOrder());
            cv.setDescription(ch.getDescription());
            cv.setResourceCount(Math.toIntExact(resourceMapper.selectCount(
                    new LambdaQueryWrapper<CourseResource>().eq(CourseResource::getChapterId, ch.getId()))));
            chapterVOs.add(cv);
        }
        vo.setChapters(chapterVOs);
        return vo;
    }

    /**
     * 创建课程
     */
    @Transactional
    public void createCourse(CourseDTO dto, Long teacherId) {
        Course course = new Course();
        course.setCourseName(dto.getCourseName());
        course.setCoverImg(dto.getCoverImg());
        course.setDescription(dto.getDescription());
        course.setTeacherId(teacherId);
        course.setStartDate(dto.getStartDate());
        course.setEndDate(dto.getEndDate());
        course.setStatus("进行中");
        course.setStudentCount(0);
        courseMapper.insert(course);
        operationLogService.record(teacherId, "课程管理", "创建课程", "COURSE", course.getId(), course.getCourseName(),
                "创建课程");
    }

    /**
     * 更新课程
     */
    public void updateCourse(Long courseId, CourseDTO dto, Long teacherId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw BusinessException.forbidden("只能编辑自己创建的课程");
        }
        course.setCourseName(dto.getCourseName());
        course.setCoverImg(dto.getCoverImg());
        course.setDescription(dto.getDescription());
        course.setStartDate(dto.getStartDate());
        course.setEndDate(dto.getEndDate());
        if (dto.getStatus() != null) {
            course.setStatus(dto.getStatus());
        }
        courseMapper.updateById(course);
        operationLogService.record(teacherId, "课程管理", "编辑课程", "COURSE", course.getId(), course.getCourseName(),
                "更新课程信息");
    }

    /**
     * 删除课程（逻辑删除）
     */
    public void deleteCourse(Long courseId, Long teacherId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw BusinessException.forbidden("只能删除自己创建的课程");
        }
        courseMapper.deleteById(courseId);
        operationLogService.record(teacherId, "课程管理", "删除课程", "COURSE", course.getId(), course.getCourseName(),
                "删除课程");
    }

    /**
     * 添加章节
     */
    public void addChapter(Long courseId, ChapterDTO dto, Long teacherId) {
        checkCourseOwner(courseId, teacherId);
        CourseChapter chapter = new CourseChapter();
        chapter.setCourseId(courseId);
        chapter.setChapterName(dto.getChapterName());
        chapter.setSortOrder(dto.getSortOrder());
        chapter.setDescription(dto.getDescription());
        chapterMapper.insert(chapter);
    }

    /**
     * 更新章节
     */
    public void updateChapter(Long courseId, Long chapterId, ChapterDTO dto, Long teacherId) {
        checkCourseOwner(courseId, teacherId);
        CourseChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw BusinessException.notFound("章节不存在");
        }
        if (!courseId.equals(chapter.getCourseId())) {
            throw BusinessException.badRequest("章节不属于该课程");
        }
        chapter.setChapterName(dto.getChapterName());
        chapter.setSortOrder(dto.getSortOrder());
        chapter.setDescription(dto.getDescription());
        chapterMapper.updateById(chapter);
    }

    /**
     * 删除章节
     */
    public void deleteChapter(Long courseId, Long chapterId, Long teacherId) {
        checkCourseOwner(courseId, teacherId);
        CourseChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw BusinessException.notFound("章节不存在");
        }
        if (!courseId.equals(chapter.getCourseId())) {
            throw BusinessException.badRequest("章节不属于该课程");
        }
        chapterMapper.deleteById(chapterId);
    }

    /**
     * 上传课程资源
     */
    public void uploadResource(Long courseId, Long chapterId, String resourceName,
            String resourceType, String fileUrl, Long fileSize, Long teacherId) {
        checkCourseOwner(courseId, teacherId);
        CourseResource resource = new CourseResource();
        resource.setCourseId(courseId);
        resource.setChapterId(chapterId);
        resource.setResourceName(resourceName);
        resource.setResourceType(resourceType);
        resource.setFileUrl(fileUrl);
        resource.setFileSize(fileSize != null ? fileSize : 0L);
        resource.setSortOrder(0);
        resourceMapper.insert(resource);
        operationLogService.record(teacherId, "课程管理", "上传资源", "COURSE_RESOURCE", resource.getId(), resourceName,
                "上传课程资源");
    }

    /**
     * 删除课程资源
     */
    public void deleteResource(Long courseId, Long resourceId, Long teacherId) {
        checkCourseOwner(courseId, teacherId);
        CourseResource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw BusinessException.notFound("资源不存在");
        }
        if (!courseId.equals(resource.getCourseId())) {
            throw BusinessException.badRequest("资源不属于该课程");
        }
        resourceMapper.deleteById(resourceId);
        operationLogService.record(teacherId, "课程管理", "删除资源", "COURSE_RESOURCE", resource.getId(), resource.getResourceName(),
                "删除课程资源");
    }

    public void markResourceViewed(Long courseId, Long resourceId, Long studentId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        checkCourseReadable(course, studentId, "ROLE_STUDENT");
        CourseResource resource = resourceMapper.selectById(resourceId);
        if (resource == null || !courseId.equals(resource.getCourseId())) {
            throw BusinessException.notFound("资源不存在");
        }
        CourseStudent cs = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId)
                .last("limit 1"));
        if (cs != null) {
            double current = cs.getProgress() == null ? 0.0 : cs.getProgress();
            cs.setProgress(Math.min(100.0, current + 3.0));
            courseStudentMapper.updateById(cs);
        }
    }

    /**
     * 查询课程资源列表
     */
    public List<CourseResource> getResources(Long courseId, Long chapterId) {
        return getResources(courseId, chapterId, null, null);
    }

    public List<CourseResource> getResources(Long courseId, Long chapterId, Long currentUserId, String role) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        checkCourseReadable(course, currentUserId, role);
        LambdaQueryWrapper<CourseResource> wrapper = new LambdaQueryWrapper<CourseResource>()
                .eq(CourseResource::getCourseId, courseId);
        if (chapterId != null) {
            wrapper.eq(CourseResource::getChapterId, chapterId);
        }
        wrapper.orderByAsc(CourseResource::getSortOrder);
        return resourceMapper.selectList(wrapper);
    }

    /**
     * 查看选课学生（教师可查看自己课程，已选课学生可查看同学）
     */
    public PageResult<StudentProfile> getStudents(Long courseId, int page, int size, Long currentUserId, String role) {
        if ("ROLE_TEACHER".equals(role)) {
            checkCourseOwner(courseId, currentUserId);
        } else if ("ROLE_STUDENT".equals(role)) {
            if (!isEnrolled(courseId, currentUserId)) {
                throw BusinessException.forbidden("请先加入课程");
            }
        }
        List<CourseStudent> enrollments = courseStudentMapper.selectList(
                new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getCourseId, courseId));
        List<Long> studentIds = enrollments.stream().map(CourseStudent::getStudentId).collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            return PageResult.of(new ArrayList<>(), 0, page, size);
        }
        Page<StudentProfile> pageResult = studentProfileMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<StudentProfile>().in(StudentProfile::getUserId, studentIds));
        pageResult.getRecords().forEach(profile -> {
            SysUser user = sysUserMapper.selectById(profile.getUserId());
            if (user != null) {
                if (!StringUtils.hasText(profile.getRealName())) {
                    profile.setRealName(user.getNickname() != null ? user.getNickname() : user.getUsername());
                }
                if (!StringUtils.hasText(profile.getStudentNo())) {
                    profile.setStudentNo(String.valueOf(user.getId()));
                }
            }
        });
        return PageResult.of(pageResult.getRecords(), pageResult.getTotal(), page, size);
    }

    /**
     * 学生选课
     */
    @Transactional
    public void enrollCourse(Long courseId, Long studentId) {
        // 1. 检查课程是否存在及时间状态
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        LocalDate today = LocalDate.now();
        if (course.getStartDate() != null && today.isBefore(course.getStartDate())) {
            throw BusinessException.badRequest("该课程尚未开始，暂不可选课");
        }
        if (course.getEndDate() != null && today.isAfter(course.getEndDate())) {
            throw BusinessException.badRequest("该课程已结束，不可选课");
        }

        // 2. 检查是否已选（未删除的记录）
        if (courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId)) != null) {
            throw BusinessException.badRequest("已选该课程，不可重复选课");
        }

        // 3. 检查是否有软删除的记录（退课后重新选课），有则物理删除旧记录再重新插入
        List<CourseStudent> deletedList = courseStudentMapper.selectListWithDeleted(courseId, studentId);
        if (!deletedList.isEmpty()) {
            // updateById 受 @TableLogic 影响会加 WHERE is_deleted=0，无法更新已被软删除的记录
            // 因此先物理删除旧记录，再重新插入
            courseStudentMapper.deletePhysically(courseId, studentId);
        }
        // 4. 全新选课
        CourseStudent cs = new CourseStudent();
        cs.setCourseId(courseId);
        cs.setStudentId(studentId);
        cs.setProgress(0.0);
        cs.setEnrolledTime(LocalDateTime.now());
        courseStudentMapper.insert(cs);

        // 5. 更新选课人数
        course.setStudentCount((course.getStudentCount() == null ? 0 : course.getStudentCount()) + 1);
        courseMapper.updateById(course);
    }

    /**
     * 学生退课
     */
    @Transactional
    public void unenrollCourse(Long courseId, Long studentId) {
        CourseStudent cs = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId));
        if (cs == null) {
            throw BusinessException.badRequest("未选该课程");
        }
        courseStudentMapper.deleteById(cs.getId());
        Course course = courseMapper.selectById(courseId);
        if (course != null) {
            course.setStudentCount(Math.max(0, course.getStudentCount() - 1));
            courseMapper.updateById(course);
        }
    }

    /**
     * 教师踢出学生
     */
    @Transactional
    public void removeStudent(Long courseId, Long studentId, Long teacherId) {
        checkCourseOwner(courseId, teacherId);
        unenrollCourse(courseId, studentId);
    }

    private void copyCourseFields(Course c, CourseVO vo) {
        vo.setId(c.getId());
        vo.setCourseName(c.getCourseName());
        vo.setCoverImg(c.getCoverImg());
        vo.setDescription(c.getDescription());
        vo.setTeacherId(c.getTeacherId());
        vo.setStartDate(c.getStartDate());
        vo.setEndDate(c.getEndDate());
        vo.setStatus(computeCourseStatus(c));
        vo.setStudentCount(c.getStudentCount());
    }

    private boolean isEnrolled(Long courseId, Long studentId) {
        if (courseId == null || studentId == null) {
            return false;
        }
        return courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId)) > 0;
    }

    private String computeCourseStatus(Course course) {
        LocalDate today = LocalDate.now();
        if (course.getStartDate() != null && today.isBefore(course.getStartDate())) {
            return "未开始";
        }
        if (course.getEndDate() != null && today.isAfter(course.getEndDate())) {
            return "已结束";
        }
        return "进行中";
    }

    private void checkCourseOwner(Long courseId, Long teacherId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw BusinessException.forbidden("只能管理自己创建的课程");
        }
    }

    private double computeTimeProgress(Course course) {
        LocalDate start = course.getStartDate();
        LocalDate end = course.getEndDate();
        if (start == null || end == null) return 0.0;
        LocalDate today = LocalDate.now();
        if (!today.isAfter(start)) return 0.0;
        if (!today.isBefore(end)) return 100.0;
        long totalDays = ChronoUnit.DAYS.between(start, end);
        if (totalDays <= 0) return 100.0;
        long elapsedDays = ChronoUnit.DAYS.between(start, today);
        return Math.min(100.0, Math.max(0.0, (double) elapsedDays / totalDays * 100.0));
    }

    private void checkCourseReadable(Course course, Long currentUserId, String role) {
        if (currentUserId == null || role == null || "ROLE_ADMIN".equals(role)) {
            return;
        }
        if ("ROLE_TEACHER".equals(role)) {
            if (!course.getTeacherId().equals(currentUserId)) {
                throw BusinessException.forbidden("只能查看自己创建的课程");
            }
            return;
        }
        if ("ROLE_STUDENT".equals(role) && !isEnrolled(course.getId(), currentUserId)) {
            throw BusinessException.forbidden("请先加入课程后再学习");
        }
    }
}
