package com.kechang.controller;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.dto.AnnouncementDTO;
import com.kechang.dto.SendMessageDTO;
import com.kechang.dto.SurveyCreateDTO;
import com.kechang.dto.SurveySubmitDTO;
import com.kechang.entity.*;
import com.kechang.service.*;
import com.kechang.vo.AdminSurveyStatsVO;
import com.kechang.vo.EvaluationVO;
import com.kechang.vo.SurveyStatsVO;
import com.kechang.vo.SurveyVO;
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

// TrainingController has been moved to com.kechang.controller.training.TrainingController

// ==================== 互动问答 ====================
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
class QAController {

    private final QAService qaService;

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ---------- 帖子 ----------
    @GetMapping("/posts")
    public Result<PageResult<QuestionPost>> listPosts(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId, @RequestParam(defaultValue = "time") String orderBy) {
        return Result.success(qaService.listPosts(page, size, courseId, orderBy));
    }

    @PostMapping("/posts")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER')")
    public Result<Void> createPost(@RequestBody QuestionPost post) {
        post.setCreateBy(uid());
        qaService.createPost(post);
        return Result.success();
    }

    @GetMapping("/posts/{id}")
    public Result<QuestionPost> getPost(@PathVariable Long id) {
        return Result.success(qaService.getPostDetail(id));
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        qaService.deletePost(id, uid());
        return Result.success();
    }

    // ---------- 回答 ----------
    @PostMapping("/posts/{id}/answer")
    public Result<Void> createAnswer(@PathVariable Long id, @RequestParam String content) {
        qaService.createAnswer(id, content, uid());
        return Result.success();
    }

    @PostMapping("/posts/{id}/answer/ai")
    public Result<Map<String, String>> aiAnswer(@PathVariable Long id) {
        return Result.success(Map.of("content", qaService.aiAnswer(id)));
    }

    @GetMapping("/posts/{id}/answers")
    public Result<PageResult<Answer>> listAnswers(@PathVariable Long id, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return Result.success(qaService.listAnswers(id, page, size));
    }

    @DeleteMapping("/answers/{id}")
    public Result<Void> deleteAnswer(@PathVariable Long id) {
        qaService.deleteAnswer(id, uid());
        return Result.success();
    }

    @PutMapping("/answers/{id}/accept")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER')")
    public Result<Void> acceptAnswer(@PathVariable Long id) {
        qaService.acceptAnswer(id);
        return Result.success();
    }

    // ---------- 评论 ----------
    @PostMapping("/answers/{id}/comment")
    public Result<Void> addCommentToAnswer(@PathVariable Long id, @RequestParam String content) {
        qaService.createComment("ANSWER", id, content, uid());
        return Result.success();
    }

    @PostMapping("/posts/{id}/comment")
    public Result<Void> addCommentToPost(@PathVariable Long id, @RequestParam String content) {
        qaService.createComment("POST", id, content, uid());
        return Result.success();
    }

    @GetMapping("/posts/{id}/comments")
    public Result<PageResult<Comment>> listPostComments(@PathVariable Long id, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return Result.success(qaService.listComments("POST", id, page, size));
    }

    @GetMapping("/answers/{id}/comments")
    public Result<PageResult<Comment>> listAnswerComments(@PathVariable Long id, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return Result.success(qaService.listComments("ANSWER", id, page, size));
    }
}

// ==================== 评价 ====================
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
class EvaluationController {

    private final EvaluationService evalService;

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String role() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().findFirst().map(Object::toString).orElse("");
    }

    @GetMapping
    public Result<PageResult<EvaluationVO>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId) {
        return Result.success(evalService.list(page, size, courseId, uid(), role()));
    }

    @GetMapping("/{id}")
    public Result<EvaluationVO> detail(@PathVariable Long id) {
        return Result.success(evalService.getDetail(id, uid()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        evalService.create(body);
        return Result.success();
    }

    /**
     * 批量提交评价结果（学生评教使用）
     */
    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id, @RequestBody List<EvaluationResult> results) {
        evalService.submitResults(id, uid(), results);
        return Result.success();
    }

    /**
     * 获取评价指标项列表
     */
    @GetMapping("/{id}/items")
    public Result<List<EvaluationItem>> items(@PathVariable Long id) {
        return Result.success(evalService.getItems(id));
    }

    /**
     * 检查当前用户是否已提交评价
     */
    @GetMapping("/{id}/check-submitted")
    public Result<Boolean> checkSubmitted(@PathVariable Long id) {
        return Result.success(evalService.checkSubmitted(id, uid()));
    }

    /**
     * 获取评价结果列表（教师/管理员查看）
     */
    /**
     * 更新评价（含指标项）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        evalService.update(id, body);
        return Result.success();
    }

    /**
     * 删除评价（含指标项和评价结果）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> delete(@PathVariable Long id) {
        evalService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/results")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<List<Map<String, Object>>> results(@PathVariable Long id) {
        return Result.success(evalService.getResultsWithItems(id));
    }
}

// ==================== 问卷 ====================
@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
class SurveyController {

    private final SurveyService surveyService;

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String role() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().findFirst().map(Object::toString).orElse("");
    }

    // ---------- 问卷 CRUD ----------
    @GetMapping
    public Result<PageResult<SurveyVO>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId) {
        return Result.success(surveyService.list(page, size, courseId, uid(), role()));
    }

    @GetMapping("/{id}")
    public Result<SurveyVO> detail(@PathVariable Long id) {
        return Result.success(surveyService.getDetail(id, uid()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> create(@Valid @RequestBody SurveyCreateDTO dto) {
        surveyService.create(dto, uid());
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SurveyCreateDTO dto) {
        surveyService.update(id, dto, uid());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> delete(@PathVariable Long id) {
        surveyService.delete(id, uid());
        return Result.success();
    }

    // ---------- 题目管理 ----------
    @GetMapping("/{id}/questions")
    public Result<List<SurveyQuestion>> questions(@PathVariable Long id) {
        return Result.success(surveyService.getQuestions(id));
    }

    @PostMapping("/{id}/questions")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> addQuestion(@PathVariable Long id, @RequestBody SurveyQuestion question) {
        surveyService.addQuestion(id, question, uid());
        return Result.success();
    }

    @PutMapping("/{id}/questions/{qid}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> updateQuestion(@PathVariable Long id, @PathVariable Long qid,
            @RequestBody SurveyQuestion question) {
        surveyService.updateQuestion(id, qid, question, uid());
        return Result.success();
    }

    @DeleteMapping("/{id}/questions/{qid}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> deleteQuestion(@PathVariable Long id, @PathVariable Long qid) {
        surveyService.deleteQuestion(id, qid, uid());
        return Result.success();
    }

    // ---------- 问卷提交 ----------
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Void> submit(@PathVariable Long id, @Valid @RequestBody SurveySubmitDTO dto) {
        surveyService.submitResponse(id, dto, uid());
        return Result.success();
    }

    @GetMapping("/{id}/my-submission")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Map<String, Object>> mySubmission(@PathVariable Long id) {
        Boolean submitted = surveyService.checkSubmitted(id, uid());
        List<SurveyResponse> responses = submitted ? surveyService.getMySubmission(id, uid()) : null;
        return Result.success(Map.of("submitted", submitted, "responses", responses != null ? responses : List.of()));
    }

    // ---------- 统计 ----------
    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    public Result<SurveyStatsVO> stats(@PathVariable Long id) {
        return Result.success(surveyService.getStats(id));
    }

    @GetMapping("/{id}/responses")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    public Result<List<SurveyResponse>> responses(@PathVariable Long id) {
        return Result.success(surveyService.getRawResponses(id));
    }

    // ---------- 管理端统计 ----------
    @GetMapping("/admin/overview")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<AdminSurveyStatsVO> adminOverview() {
        return Result.success(surveyService.getAdminStats());
    }
}

// ==================== 知识库与笔记 ====================
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
class KnowledgeController {

    private final KnowledgeService knowledgeService;

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/knowledge/articles")
    public Result<PageResult<KnowledgeArticle>> listArticles(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String keyword) {
        return Result.success(knowledgeService.listArticles(page, size, keyword));
    }

    @GetMapping("/knowledge/articles/{id}")
    public Result<KnowledgeArticle> getArticle(@PathVariable Long id) {
        return Result.success(knowledgeService.getArticle(id));
    }

    @PostMapping("/knowledge/articles")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> createArticle(@RequestBody KnowledgeArticle a) {
        a.setCreateBy(uid());
        knowledgeService.createArticle(a);
        return Result.success();
    }

    @GetMapping("/notes")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<PageResult<LearningNote>> listNotes(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return Result.success(knowledgeService.listNotes(uid(), page, size));
    }

    @PostMapping("/notes")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Void> createNote(@RequestBody LearningNote n) {
        n.setStudentId(uid());
        knowledgeService.createNote(n);
        return Result.success();
    }

    @PutMapping("/notes/{id}")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Void> updateNote(@PathVariable Long id, @RequestBody LearningNote n) {
        knowledgeService.updateNote(id, n);
        return Result.success();
    }

    @DeleteMapping("/notes/{id}")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Void> deleteNote(@PathVariable Long id) {
        knowledgeService.deleteNote(id);
        return Result.success();
    }

    @PutMapping("/notes/{id}/public")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Void> togglePublic(@PathVariable Long id) {
        knowledgeService.togglePublic(id);
        return Result.success();
    }
}

// ==================== 通知 ====================
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
class NotificationController {

    private final NotificationService notifService;

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return Result.success(notifService.list(uid(), page, size));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(notifService.getDetail(id, uid()));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(notifService.unreadCount(uid()));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notifService.markRead(id, uid());
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notifService.markAllRead(uid());
        return Result.success();
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    public Result<Void> send(@RequestBody Notification n) {
        n.setSenderId(uid());
        notifService.send(n);
        return Result.success();
    }
}

// ==================== 公告 ====================
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
class AnnouncementController {

    private final NotificationService notifService;

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String role() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().findFirst().map(Object::toString).orElse("");
    }

    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId) {
        return Result.success(notifService.listAnnouncements(page, size, courseId, uid(), role()));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(notifService.getAnnouncementDetail(id, uid(), role()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> create(@Valid @RequestBody AnnouncementDTO dto) {
        notifService.createAnnouncement(dto, uid());
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AnnouncementDTO dto) {
        notifService.updateAnnouncement(id, dto, uid());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> delete(@PathVariable Long id) {
        notifService.deleteAnnouncement(id, uid());
        return Result.success();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Void> confirm(@PathVariable Long id) {
        notifService.confirmAnnouncement(id, uid());
        return Result.success();
    }

    @GetMapping("/{id}/confirms")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<PageResult<Map<String, Object>>> confirmList(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(notifService.getConfirmList(id, page, size, uid()));
    }
}

// ==================== 通用文件上传 ====================
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
class UploadController {

    private final FileProcessingService fileProcessingService;

    @Value("${kechang.upload.path:./uploads}")
    private String uploadBasePath;

    @PostMapping("/file")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_STUDENT')")
    public Result<Map<String, Object>> uploadFile(@RequestPart("file") MultipartFile file) throws IOException {
        Path baseDir = Paths.get(uploadBasePath).toAbsolutePath().normalize();
        Path commonDir = baseDir.resolve("common");
        Files.createDirectories(commonDir);

        String safeName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replace("\\", "/").replaceAll(".*/", "")
                : "file";
        String uuid = UUID.randomUUID().toString();
        String fileName = uuid + "_" + safeName;
        Path destFile = commonDir.resolve(fileName);
        Files.copy(file.getInputStream(), destFile, StandardCopyOption.REPLACE_EXISTING);

        String url = "/uploads/common/" + fileName;
        return Result.success(Map.of("url", url, "name", safeName, "size", Files.size(destFile)));
    }

    @GetMapping("/preview")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_STUDENT')")
    public Result<Map<String, Object>> preview(@RequestParam String url,
            @RequestParam(required = false) String name) {
        String previewUrl = fileProcessingService.ensurePdfPreview(url, name);
        return Result.success(Map.of("url", previewUrl));
    }

    @GetMapping("/pdf-pages")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_STUDENT')")
    public Result<Map<String, Object>> pdfPages(@RequestParam String url,
            @RequestParam(required = false) String name) {
        return Result.success(fileProcessingService.renderPdfPages(url, name));
    }
}

// ==================== 私信 ====================
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
class MessageController {

    private final MessageService messageService;

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String role() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().findFirst().map(Object::toString).orElse("");
    }

    /** 会话列表 */
    @GetMapping("/conversations")
    public Result<PageResult<Map<String, Object>>> listConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(messageService.listConversations(uid(), page, size));
    }

    /** 与某用户的聊天记录 */
    @GetMapping("/with/{otherUserId}")
    public Result<PageResult<Map<String, Object>>> getMessages(
            @PathVariable Long otherUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        return Result.success(messageService.getMessages(uid(), otherUserId, page, size));
    }

    /** 发送消息 */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_STUDENT')")
    public Result<Map<String, Object>> send(@Valid @RequestBody SendMessageDTO dto) {
        return Result.success(messageService.sendMessage(uid(), dto));
    }

    /** 未读消息总数 */
    @GetMapping("/unread-total")
    public Result<Long> unreadTotal() {
        return Result.success(messageService.unreadTotal(uid()));
    }

    /** 联系人列表 */
    @GetMapping("/contacts")
    public Result<List<Map<String, Object>>> getContacts(
            @RequestParam(required = false) String keyword) {
        return Result.success(messageService.getContacts(uid(), role(), keyword));
    }
}
