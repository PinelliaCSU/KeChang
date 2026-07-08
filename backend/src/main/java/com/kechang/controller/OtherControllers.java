package com.kechang.controller;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.dto.SurveyCreateDTO;
import com.kechang.dto.SurveySubmitDTO;
import com.kechang.entity.*;
import com.kechang.service.*;
import com.kechang.vo.AdminSurveyStatsVO;
import com.kechang.vo.SurveyStatsVO;
import com.kechang.vo.SurveyVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
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

// ==================== 实训管理 ====================
@RestController
@RequestMapping("/api/trainings")
@RequiredArgsConstructor
class TrainingController {

    private final TrainingService trainingService;

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public Result<PageResult<TrainingProject>> listProjects(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId) {
        return Result.success(trainingService.listProjects(page, size, courseId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> createProject(@RequestBody TrainingProject p) {
        p.setCreateBy(uid());
        trainingService.createProject(p);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> updateProject(@PathVariable Long id, @RequestBody TrainingProject p) {
        trainingService.updateProject(id, p);
        return Result.success();
    }

    @PostMapping("/{id}/tasks")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> createTask(@RequestBody TrainingTask t) {
        trainingService.createTask(t);
        return Result.success();
    }

    @GetMapping("/{id}/tasks")
    public Result<PageResult<TrainingTask>> listTasks(@PathVariable Long id, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return Result.success(trainingService.listTasks(id, page, size));
    }

    @PostMapping("/tasks/{taskId}/report")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Void> submitReport(@PathVariable Long taskId, @RequestParam(required = false) String content, @RequestParam(required = false) String attachmentUrl) {
        trainingService.submitReport(taskId, uid(), content, attachmentUrl);
        return Result.success();
    }

    @GetMapping("/tasks/{taskId}/reports")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<PageResult<TrainingReport>> listReports(@PathVariable Long taskId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return Result.success(trainingService.listReports(taskId, page, size));
    }

    @PutMapping("/reports/{reportId}/grade")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> gradeReport(@PathVariable Long reportId, @RequestParam Double score, @RequestParam(required = false) String comment) {
        trainingService.gradeReport(reportId, score, comment);
        return Result.success();
    }
}

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

    @GetMapping
    public Result<PageResult<Evaluation>> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) Long courseId) {
        return Result.success(evalService.list(page, size, courseId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> create(@RequestBody Evaluation e) {
        evalService.create(e);
        return Result.success();
    }

    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id, @RequestBody EvaluationResult r) {
        r.setEvaluatorId(uid());
        evalService.submitResult(r);
        return Result.success();
    }

    @GetMapping("/{id}/results")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<List<EvaluationResult>> results(@PathVariable Long id) {
        return Result.success(evalService.getResults(id));
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
    public Result<PageResult<Notification>> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return Result.success(notifService.list(uid(), page, size));
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

// ==================== 通用文件上传 ====================
@RestController
@RequestMapping("/api/upload")
class UploadController {

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
}
