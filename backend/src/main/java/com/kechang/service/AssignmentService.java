package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.dto.AssignmentDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.vo.AssignmentSubmitVO;
import com.kechang.vo.AssignmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 作业管理服务
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final AssignmentSubmitMapper submitMapper;
    private final AssignmentGradeMapper gradeMapper;
    private final AttachmentFileMapper attachmentFileMapper;
    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final GradeService gradeService;
    private final FileProcessingService fileProcessingService;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    @Value("${kechang.upload.path:./uploads}")
    private String uploadBasePath;

    public PageResult<AssignmentVO> list(int page, int size, Long courseId, Long chapterId, Long userId, String role) {
        LambdaQueryWrapper<Assignment> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(Assignment::getCourseId, courseId);
        }
        if (chapterId != null) {
            w.eq(Assignment::getChapterId, chapterId);
        }
        if ("ROLE_TEACHER".equals(role)) {
            w.eq(Assignment::getCreateBy, userId);
        }
        if ("ROLE_STUDENT".equals(role)) {
            List<Long> courseIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                            .eq(CourseStudent::getStudentId, userId))
                    .stream()
                    .map(CourseStudent::getCourseId)
                    .collect(Collectors.toList());
            List<Long> submittedAssignmentIds = submitMapper.selectList(new LambdaQueryWrapper<AssignmentSubmit>()
                            .eq(AssignmentSubmit::getStudentId, userId))
                    .stream()
                    .map(AssignmentSubmit::getAssignmentId)
                    .collect(Collectors.toList());
            if (courseIds.isEmpty() && submittedAssignmentIds.isEmpty()) {
                return PageResult.of(new ArrayList<>(), 0, page, size);
            }
            w.and(q -> {
                if (!courseIds.isEmpty()) {
                    q.in(Assignment::getCourseId, courseIds);
                }
                if (!submittedAssignmentIds.isEmpty()) {
                    if (!courseIds.isEmpty()) {
                        q.or();
                    }
                    q.in(Assignment::getId, submittedAssignmentIds);
                }
            });
        }
        w.orderByDesc(Assignment::getCreateTime);
        Page<Assignment> p = assignmentMapper.selectPage(new Page<>(page, size), w);
        Long studentId = "ROLE_STUDENT".equals(role) ? userId : null;
        List<AssignmentVO> records = p.getRecords().stream()
                .map(assignment -> toAssignmentVO(assignment, studentId))
                .collect(Collectors.toList());
        return PageResult.of(records, p.getTotal(), page, size);
    }

    public void create(AssignmentDTO dto, Long teacherId) {
        // 如果传了多文件但没传单个 URL，用第一个文件的 URL
        String effectiveUrl = dto.getAttachmentUrl();
        if ((effectiveUrl == null || effectiveUrl.isEmpty()) && dto.getAttachmentUrls() != null && !dto.getAttachmentUrls().isEmpty()) {
            effectiveUrl = dto.getAttachmentUrls().get(0);
        }

        Assignment a = new Assignment();
        a.setCourseId(dto.getCourseId());
        a.setChapterId(dto.getChapterId());
        a.setTitle(dto.getTitle());
        a.setContent(dto.getContent());
        a.setAttachmentUrl(effectiveUrl);
        a.setDeadline(dto.getDeadline());
        a.setTotalScore(dto.getTotalScore() != null ? dto.getTotalScore() : 100.0);
        a.setStatus("进行中");
        a.setCreateBy(teacherId);
        assignmentMapper.insert(a);

        // 保存多文件附件
        saveAttachments("ASSIGNMENT", a.getId(), dto.getAttachmentUrls(), dto.getAttachmentNames(), dto.getAttachmentSizes());
        Course course = courseMapper.selectById(a.getCourseId());
        notificationService.sendToCourseStudents(a.getCourseId(), teacherId, "作业",
                "新作业：" + a.getTitle(),
                "课程《" + (course != null ? course.getCourseName() : "课程") + "》发布了新作业，请按时完成。");
        operationLogService.record(teacherId, "作业管理", "发布作业", "ASSIGNMENT", a.getId(), a.getTitle(),
                "发布课程作业");
    }

    public AssignmentVO getDetail(Long id, Long userId, String role) {
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment == null) {
            throw BusinessException.notFound("作业不存在");
        }
        checkAssignmentReadable(assignment, userId, role);
        return toAssignmentVO(assignment, "ROLE_STUDENT".equals(role) ? userId : null);
    }

    public void update(Long id, AssignmentDTO dto, Long teacherId) {
        Assignment a = assignmentMapper.selectById(id);
        if (a == null) {
            throw BusinessException.notFound("作业不存在");
        }
        if (!teacherId.equals(a.getCreateBy())) {
            throw BusinessException.forbidden("只能编辑自己发布的作业");
        }
        String effectiveUrl = dto.getAttachmentUrl();
        if ((effectiveUrl == null || effectiveUrl.isEmpty()) && dto.getAttachmentUrls() != null && !dto.getAttachmentUrls().isEmpty()) {
            effectiveUrl = dto.getAttachmentUrls().get(0);
        }
        a.setTitle(dto.getTitle());
        a.setContent(dto.getContent());
        a.setChapterId(dto.getChapterId());
        a.setDeadline(dto.getDeadline());
        a.setAttachmentUrl(effectiveUrl);
        a.setTotalScore(dto.getTotalScore() != null ? dto.getTotalScore() : a.getTotalScore());
        assignmentMapper.updateById(a);

        // 更新附件：先删旧附件，再保存新的
        attachmentFileMapper.delete(new LambdaQueryWrapper<AttachmentFile>()
                .eq(AttachmentFile::getTargetType, "ASSIGNMENT")
                .eq(AttachmentFile::getTargetId, id));
        saveAttachments("ASSIGNMENT", id, dto.getAttachmentUrls(), dto.getAttachmentNames(), dto.getAttachmentSizes());
        operationLogService.record(a.getCreateBy(), "作业管理", "编辑作业", "ASSIGNMENT", a.getId(), a.getTitle(),
                "更新作业信息");
    }

    public void delete(Long id, Long teacherId) {
        Assignment a = assignmentMapper.selectById(id);
        if (a == null) {
            throw BusinessException.notFound("作业不存在");
        }
        if (!teacherId.equals(a.getCreateBy())) {
            throw BusinessException.forbidden("只能删除自己发布的作业");
        }
        assignmentMapper.deleteById(id);
        operationLogService.record(a.getCreateBy(), "作业管理", "删除作业", "ASSIGNMENT", a.getId(), a.getTitle(),
                "删除作业");
    }

    private void saveAttachments(String targetType, Long targetId, List<String> urls, List<String> names, List<Long> sizes) {
        if (urls == null || urls.isEmpty()) return;
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            if (url == null || url.isEmpty()) continue;
            AttachmentFile af = new AttachmentFile();
            af.setTargetType(targetType);
            af.setTargetId(targetId);
            af.setFileUrl(url);
            af.setFileName(names != null && i < names.size() ? names.get(i) : url.substring(url.lastIndexOf('/') + 1));
            af.setFileSize(sizes != null && i < sizes.size() ? sizes.get(i) : 0L);
            af.setSortOrder(i);
            attachmentFileMapper.insert(af);
        }
    }

    private List<AttachmentFile> getAttachments(String targetType, Long targetId) {
        return attachmentFileMapper.selectList(new LambdaQueryWrapper<AttachmentFile>()
                .eq(AttachmentFile::getTargetType, targetType)
                .eq(AttachmentFile::getTargetId, targetId)
                .orderByAsc(AttachmentFile::getSortOrder));
    }

    @Transactional
    public void submit(Long assignmentId, Long studentId, String content, String attachmentUrl,
            List<String> attachmentUrls, List<String> attachmentNames, List<Long> attachmentSizes) {
        Assignment a = assignmentMapper.selectById(assignmentId);
        if (a == null) {
            throw BusinessException.notFound("作业不存在");
        }
        if (a.getDeadline() != null && LocalDateTime.now().isAfter(a.getDeadline())) {
            throw BusinessException.badRequest("已超过提交截止时间");
        }
        CourseStudent enrollment = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, a.getCourseId())
                .eq(CourseStudent::getStudentId, studentId));
        if (enrollment == null) {
            throw BusinessException.forbidden("未选该课程，不能提交该课程作业");
        }

        AssignmentSubmit exist = submitMapper.selectOne(new LambdaQueryWrapper<AssignmentSubmit>()
                .eq(AssignmentSubmit::getAssignmentId, assignmentId)
                .eq(AssignmentSubmit::getStudentId, studentId)
                .orderByDesc(AssignmentSubmit::getSubmitTime)
                .last("limit 1"));
        if (exist != null) {
            throw BusinessException.badRequest("已提交过该作业");
        }

        // 如果传了多文件但没传单个 URL，用第一个文件的 URL
        String effectiveUrl = attachmentUrl;
        if ((effectiveUrl == null || effectiveUrl.isEmpty()) && attachmentUrls != null && !attachmentUrls.isEmpty()) {
            effectiveUrl = attachmentUrls.get(0);
        }

        AssignmentSubmit s = new AssignmentSubmit();
        s.setAssignmentId(assignmentId);
        s.setStudentId(studentId);
        s.setContent(content);
        s.setAttachmentUrl(effectiveUrl);
        s.setSubmitTime(LocalDateTime.now());
        s.setStatus("待批改");
        submitMapper.insert(s);

        // 保存多文件附件
        saveAttachments("SUBMIT", s.getId(), attachmentUrls, attachmentNames, attachmentSizes);
        increaseCourseProgress(a.getCourseId(), studentId, 10.0);
    }

    public PageResult<AssignmentSubmitVO> getSubmitList(Long assignmentId, int page, int size, Long teacherId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw BusinessException.notFound("作业不存在");
        }
        if (!teacherId.equals(assignment.getCreateBy())) {
            throw BusinessException.forbidden("只能查看自己发布作业的提交记录");
        }
        Page<AssignmentSubmit> p = submitMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AssignmentSubmit>()
                        .eq(AssignmentSubmit::getAssignmentId, assignmentId)
                        .orderByDesc(AssignmentSubmit::getSubmitTime));
        List<AssignmentSubmitVO> records = p.getRecords().stream()
                .map(this::toSubmitVO)
                .collect(Collectors.toList());
        return PageResult.of(records, p.getTotal(), page, size);
    }

    @Transactional
    public void grade(Long submitId, Double score, String comment, String feedbackMarks, Boolean isExcellent, Long teacherId) {
        AssignmentSubmit s = submitMapper.selectById(submitId);
        if (s == null) {
            throw BusinessException.notFound("提交记录不存在");
        }
        Assignment assignment = assignmentMapper.selectById(s.getAssignmentId());
        if (assignment == null) {
            throw BusinessException.notFound("作业不存在");
        }
        if (!teacherId.equals(assignment.getCreateBy())) {
            throw BusinessException.forbidden("只能批改自己发布作业的提交");
        }
        if (score == null || score < 0 || score > 100) {
            throw BusinessException.badRequest("评分必须在 0 到 100 之间");
        }
        s.setScore(score);
        s.setIsExcellent(Boolean.TRUE.equals(isExcellent));
        s.setStatus("已批改");
        submitMapper.updateById(s);

        AssignmentGrade g = gradeMapper.selectOne(new LambdaQueryWrapper<AssignmentGrade>()
                .eq(AssignmentGrade::getSubmitId, submitId));
        if (g == null) {
            g = new AssignmentGrade();
            g.setSubmitId(submitId);
        }
        g.setScore(score);
        g.setComment(comment);
        g.setFeedbackMarks(feedbackMarks);
        g.setGradedBy(teacherId);
        g.setGradedTime(LocalDateTime.now());
        if (g.getId() == null) {
            gradeMapper.insert(g);
        } else {
            gradeMapper.updateById(g);
        }

        attachmentFileMapper.delete(new LambdaQueryWrapper<AttachmentFile>()
                .eq(AttachmentFile::getTargetType, "FEEDBACK_SUBMIT")
                .eq(AttachmentFile::getTargetId, submitId));
        List<AttachmentFile> feedbackFiles = fileProcessingService.buildAnnotatedFiles(
                submitId,
                submitFilesForFeedback(s),
                feedbackMarks
        );
        for (AttachmentFile feedbackFile : feedbackFiles) {
            attachmentFileMapper.insert(feedbackFile);
        }

        gradeService.recalculateGrade(assignment.getCourseId(), s.getStudentId());
        notificationService.sendToUser(s.getStudentId(), teacherId, assignment.getCourseId(), "作业",
                "作业已批改：" + assignment.getTitle(),
                "你的作业《" + assignment.getTitle() + "》已完成批改，得分：" + score + "。");
        operationLogService.record(teacherId, "作业管理", "批改作业", "SUBMIT", s.getId(), assignment.getTitle(),
                "批改学生作业并给出成绩");
    }

    public void writeSubmitArchive(Long assignmentId, Long teacherId, OutputStream outputStream) throws IOException {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw BusinessException.notFound("作业不存在");
        }
        if (!teacherId.equals(assignment.getCreateBy())) {
            throw BusinessException.forbidden("只能下载自己发布作业的提交附件");
        }
        List<AssignmentSubmit> submitList = submitMapper.selectList(new LambdaQueryWrapper<AssignmentSubmit>()
                .eq(AssignmentSubmit::getAssignmentId, assignmentId)
                .orderByAsc(AssignmentSubmit::getStudentId));
        try (ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (AssignmentSubmit submit : submitList) {
                StudentProfile profile = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getUserId, submit.getStudentId())
                        .last("limit 1"));
                String folder = studentArchiveName(profile, submit);
                List<AttachmentFile> files = getAttachments("SUBMIT", submit.getId());
                if (files.isEmpty() && submit.getAttachmentUrl() != null) {
                    AttachmentFile file = new AttachmentFile();
                    file.setFileUrl(submit.getAttachmentUrl());
                    file.setFileName(submit.getAttachmentUrl().substring(submit.getAttachmentUrl().lastIndexOf('/') + 1));
                    files = List.of(file);
                }
                if (files.isEmpty()) {
                    zip.putNextEntry(new ZipEntry(folder + "/未上传附件.txt"));
                    zip.write("该学生没有上传附件。".getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                    continue;
                }
                for (int i = 0; i < files.size(); i++) {
                    AttachmentFile file = files.get(i);
                    Path path = resolveUploadPath(file.getFileUrl());
                    if (!Files.exists(path)) {
                        continue;
                    }
                    String entryName = folder + "/" + (i + 1) + "-" + safeArchivePart(file.getFileName());
                    zip.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zip);
                    zip.closeEntry();
                }
            }
        }
    }

    public String archiveFileName(Long assignmentId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        return fileProcessingService.archiveFileName(assignment != null ? assignment.getTitle() : "作业附件");
    }

    private AssignmentVO toAssignmentVO(Assignment assignment, Long studentId) {
        AssignmentVO vo = new AssignmentVO();
        vo.setId(assignment.getId());
        vo.setCourseId(assignment.getCourseId());
        vo.setChapterId(assignment.getChapterId());
        vo.setTitle(assignment.getTitle());
        vo.setContent(assignment.getContent());
        vo.setAttachmentUrl(assignment.getAttachmentUrl());
        vo.setAttachments(getAttachments("ASSIGNMENT", assignment.getId()));
        vo.setDeadline(assignment.getDeadline());
        vo.setTotalScore(assignment.getTotalScore());
        vo.setStatus(computeAssignmentStatus(assignment, studentId));
        vo.setCreateBy(assignment.getCreateBy());
        vo.setCreateTime(assignment.getCreateTime());
        vo.setExcellentSubmits(getExcellentSubmits(assignment.getId()));

        Course course = courseMapper.selectById(assignment.getCourseId());
        if (course != null) {
            vo.setCourseName(course.getCourseName());
        }

        Long submitCount = submitMapper.selectCount(new LambdaQueryWrapper<AssignmentSubmit>()
                .eq(AssignmentSubmit::getAssignmentId, assignment.getId()));
        Long enrolledCount = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, assignment.getCourseId()));
        vo.setSubmitCount(Math.toIntExact(submitCount));
        vo.setTotalStudents(Math.toIntExact(Math.max(enrolledCount, submitCount)));
        fillMySubmission(vo, assignment.getId(), studentId);
        return vo;
    }

    private void fillMySubmission(AssignmentVO vo, Long assignmentId, Long studentId) {
        if (studentId == null) {
            return;
        }
        AssignmentSubmit submit = submitMapper.selectOne(new LambdaQueryWrapper<AssignmentSubmit>()
                .eq(AssignmentSubmit::getAssignmentId, assignmentId)
                .eq(AssignmentSubmit::getStudentId, studentId)
                .orderByDesc(AssignmentSubmit::getSubmitTime)
                .last("limit 1"));
        vo.setSubmitted(submit != null);
        if (submit == null) {
            return;
        }
        vo.setMySubmitId(submit.getId());
        vo.setSubmitContent(submit.getContent());
        vo.setSubmitAttachmentUrl(submit.getAttachmentUrl());
        vo.setSubmitAttachments(getAttachments("SUBMIT", submit.getId()));
        vo.setFeedbackAttachments(getAttachments("FEEDBACK_SUBMIT", submit.getId()));
        vo.setSubmitTime(submit.getSubmitTime());
        vo.setMySubmitStatus(submit.getStatus());
        vo.setMyScore(submit.getScore());

        AssignmentGrade grade = gradeMapper.selectOne(new LambdaQueryWrapper<AssignmentGrade>()
                .eq(AssignmentGrade::getSubmitId, submit.getId())
                .last("limit 1"));
        if (grade != null) {
            vo.setMyScore(grade.getScore());
            vo.setGradeComment(grade.getComment());
            vo.setFeedbackMarks(grade.getFeedbackMarks());
            vo.setGradedTime(grade.getGradedTime());
        }
    }

    private AssignmentSubmitVO toSubmitVO(AssignmentSubmit submit) {
        AssignmentSubmitVO vo = new AssignmentSubmitVO();
        vo.setId(submit.getId());
        vo.setAssignmentId(submit.getAssignmentId());
        vo.setStudentId(submit.getStudentId());
        vo.setContent(submit.getContent());
        vo.setAttachmentUrl(submit.getAttachmentUrl());
        vo.setSubmitTime(submit.getSubmitTime());
        vo.setScore(submit.getScore());
        vo.setStatus(submit.getStatus());
        vo.setIsExcellent(Boolean.TRUE.equals(submit.getIsExcellent()));
        vo.setCreateTime(submit.getCreateTime());
        vo.setAttachments(getAttachments("SUBMIT", submit.getId()));
        vo.setFeedbackAttachments(getAttachments("FEEDBACK_SUBMIT", submit.getId()));

        AssignmentGrade grade = gradeMapper.selectOne(new LambdaQueryWrapper<AssignmentGrade>()
                .eq(AssignmentGrade::getSubmitId, submit.getId())
                .last("limit 1"));
        if (grade != null) {
            vo.setScore(grade.getScore());
            vo.setComment(grade.getComment());
            vo.setFeedbackMarks(grade.getFeedbackMarks());
            vo.setGradedTime(grade.getGradedTime());
        }

        StudentProfile profile = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, submit.getStudentId())
                .last("limit 1"));
        if (profile != null) {
            vo.setStudentName(profile.getRealName());
            vo.setStudentNo(profile.getStudentNo());
            vo.setStudentMajor(profile.getMajor());
            vo.setClassName(profile.getClassName());
        }
        if (vo.getStudentName() == null) {
            SysUser user = sysUserMapper.selectById(submit.getStudentId());
            if (user != null) {
                vo.setStudentName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        }
        return vo;
    }

    private String computeAssignmentStatus(Assignment assignment, Long studentId) {
        if (studentId != null) {
            AssignmentSubmit submit = submitMapper.selectOne(new LambdaQueryWrapper<AssignmentSubmit>()
                    .eq(AssignmentSubmit::getAssignmentId, assignment.getId())
                    .eq(AssignmentSubmit::getStudentId, studentId)
                    .orderByDesc(AssignmentSubmit::getSubmitTime)
                    .last("limit 1"));
            if (submit != null && "已批改".equals(submit.getStatus())) {
                return "已批改";
            }
        }
        if (assignment.getCreateTime() != null && LocalDateTime.now().isBefore(assignment.getCreateTime())) {
            return "未开始";
        }
        if (assignment.getDeadline() != null && LocalDateTime.now().isAfter(assignment.getDeadline())) {
            return "已截止";
        }
        return "进行中";
    }

    private void checkAssignmentReadable(Assignment assignment, Long userId, String role) {
        if ("ROLE_TEACHER".equals(role)) {
            if (!userId.equals(assignment.getCreateBy())) {
                throw BusinessException.forbidden("只能查看自己发布的作业");
            }
            return;
        }
        if ("ROLE_STUDENT".equals(role)) {
            CourseStudent enrollment = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, assignment.getCourseId())
                    .eq(CourseStudent::getStudentId, userId));
            if (enrollment == null) {
                throw BusinessException.forbidden("请先加入课程后再查看作业");
            }
        }
    }

    private void increaseCourseProgress(Long courseId, Long studentId, double delta) {
        CourseStudent cs = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId)
                .last("limit 1"));
        if (cs == null) {
            return;
        }
        double current = cs.getProgress() == null ? 0.0 : cs.getProgress();
        cs.setProgress(Math.min(100.0, current + delta));
        courseStudentMapper.updateById(cs);
    }

    private List<AssignmentSubmitVO> getExcellentSubmits(Long assignmentId) {
        return submitMapper.selectList(new LambdaQueryWrapper<AssignmentSubmit>()
                        .eq(AssignmentSubmit::getAssignmentId, assignmentId)
                        .eq(AssignmentSubmit::getIsExcellent, true)
                        .orderByDesc(AssignmentSubmit::getSubmitTime))
                .stream()
                .map(this::toSubmitVO)
                .collect(Collectors.toList());
    }

    private List<AttachmentFile> submitFilesForFeedback(AssignmentSubmit submit) {
        List<AttachmentFile> files = getAttachments("SUBMIT", submit.getId());
        if (!files.isEmpty() || submit.getAttachmentUrl() == null || submit.getAttachmentUrl().isBlank()) {
            return files;
        }
        AttachmentFile fallback = new AttachmentFile();
        fallback.setTargetType("SUBMIT");
        fallback.setTargetId(submit.getId());
        fallback.setFileUrl(submit.getAttachmentUrl());
        fallback.setFileName(submit.getAttachmentUrl().substring(submit.getAttachmentUrl().lastIndexOf('/') + 1));
        fallback.setFileSize(0L);
        fallback.setSortOrder(0);
        return List.of(fallback);
    }

    private String studentArchiveName(StudentProfile profile, AssignmentSubmit submit) {
        String studentNo = profile != null && profile.getStudentNo() != null ? profile.getStudentNo() : String.valueOf(submit.getStudentId());
        String name = profile != null && profile.getRealName() != null ? profile.getRealName() : "学生";
        String major = profile != null && profile.getMajor() != null ? profile.getMajor() : "未填专业";
        return safeArchivePart(studentNo + "-" + name + "-" + major);
    }

    private String safeArchivePart(String value) {
        return (value == null ? "file" : value).replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_");
    }

    private Path resolveUploadPath(String fileUrl) {
        String value = fileUrl == null ? "" : fileUrl;
        int uploads = value.indexOf("/uploads/");
        String relative = uploads >= 0 ? value.substring(uploads + "/uploads/".length()) : value.replaceFirst("^/+", "");
        return Paths.get(uploadBasePath).toAbsolutePath().normalize().resolve(relative).normalize();
    }
}
