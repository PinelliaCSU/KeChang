package com.kechang.controller;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.entity.*;
import com.kechang.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> createProject(@RequestBody TrainingProject p) {
        p.setCreateBy(uid());
        trainingService.createProject(p);
        return Result.success("创建成功");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> updateProject(@PathVariable Long id, @RequestBody TrainingProject p) {
        trainingService.updateProject(id, p);
        return Result.success("更新成功");
    }

    @PostMapping("/{id}/tasks")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> createTask(@RequestBody TrainingTask t) {
        trainingService.createTask(t);
        return Result.success("任务添加成功");
    }

    @GetMapping("/{id}/tasks")
    public Result<PageResult<TrainingTask>> listTasks(@PathVariable Long id, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return Result.success(trainingService.listTasks(id, page, size));
    }

    @PostMapping("/tasks/{taskId}/report")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public Result<Void> submitReport(@PathVariable Long taskId, @RequestParam(required = false) String content, @RequestParam(required = false) String attachmentUrl) {
        trainingService.submitReport(taskId, uid(), content, attachmentUrl);
        return Result.success("报告提交成功");
    }

    @GetMapping("/tasks/{taskId}/reports")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<PageResult<TrainingReport>> listReports(@PathVariable Long taskId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return Result.success(trainingService.listReports(taskId, page, size));
    }

    @PutMapping("/reports/{reportId}/grade")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> gradeReport(@PathVariable Long reportId, @RequestParam Double score, @RequestParam(required = false) String comment) {
        trainingService.gradeReport(reportId, score, comment);
        return Result.success("评分成功");
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

    @GetMapping("/posts")
    public Result<PageResult<QuestionPost>> listPosts(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId, @RequestParam(defaultValue = "time") String orderBy) {
        return Result.success(qaService.listPosts(page, size, courseId, orderBy));
    }

    @PostMapping("/posts")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public Result<Void> createPost(@RequestBody QuestionPost post) {
        post.setCreateBy(uid());
        qaService.createPost(post);
        return Result.success("发布成功");
    }

    @GetMapping("/posts/{id}")
    public Result<QuestionPost> getPost(@PathVariable Long id) {
        return Result.success(qaService.getPostDetail(id));
    }

    @PostMapping("/posts/{id}/answer")
    public Result<Void> createAnswer(@PathVariable Long id, @RequestParam String content) {
        qaService.createAnswer(id, content, uid());
        return Result.success("回答成功");
    }

    @PostMapping("/posts/{id}/answer/ai")
    public Result<Map<String, String>> aiAnswer(@PathVariable Long id) {
        return Result.success(Map.of("content", qaService.aiAnswer(id)));
    }

    @PutMapping("/answers/{id}/accept")
    @PreAuthorize("hasAnyRole('ROLE_STUDENT','ROLE_TEACHER')")
    public Result<Void> acceptAnswer(@PathVariable Long id) {
        qaService.acceptAnswer(id);
        return Result.success("已采纳");
    }

    @PostMapping("/answers/{id}/comment")
    public Result<Void> addComment(@PathVariable Long id, @RequestParam String content) {
        qaService.createComment("ANSWER", id, content, uid());
        return Result.success("评论成功");
    }

    @GetMapping("/posts/{id}/answers")
    public Result<PageResult<Answer>> listAnswers(@PathVariable Long id, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return Result.success(qaService.listAnswers(id, page, size));
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
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> create(@RequestBody Evaluation e) {
        evalService.create(e);
        return Result.success("创建成功");
    }

    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id, @RequestBody EvaluationResult r) {
        r.setEvaluatorId(uid());
        evalService.submitResult(r);
        return Result.success("提交成功");
    }

    @GetMapping("/{id}/results")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
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

    @GetMapping
    public Result<PageResult<Survey>> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) Long courseId) {
        return Result.success(surveyService.list(page, size, courseId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> create(@RequestBody Survey s) {
        s.setCreateBy(uid());
        surveyService.create(s);
        return Result.success("创建成功");
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public Result<Void> submit(@PathVariable Long id, @RequestBody SurveyResponse r) {
        r.setRespondentId(uid());
        surveyService.submitResponse(r);
        return Result.success("提交成功");
    }

    @GetMapping("/{id}/questions")
    public Result<List<SurveyQuestion>> questions(@PathVariable Long id) {
        return Result.success(surveyService.getQuestions(id));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<List<SurveyResponse>> stats(@PathVariable Long id) {
        return Result.success(surveyService.getStats(id));
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
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> createArticle(@RequestBody KnowledgeArticle a) {
        a.setCreateBy(uid());
        knowledgeService.createArticle(a);
        return Result.success("发布成功");
    }

    @GetMapping("/notes")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public Result<PageResult<LearningNote>> listNotes(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return Result.success(knowledgeService.listNotes(uid(), page, size));
    }

    @PostMapping("/notes")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public Result<Void> createNote(@RequestBody LearningNote n) {
        n.setStudentId(uid());
        knowledgeService.createNote(n);
        return Result.success("笔记创建成功");
    }

    @PutMapping("/notes/{id}")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public Result<Void> updateNote(@PathVariable Long id, @RequestBody LearningNote n) {
        knowledgeService.updateNote(id, n);
        return Result.success("更新成功");
    }

    @DeleteMapping("/notes/{id}")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public Result<Void> deleteNote(@PathVariable Long id) {
        knowledgeService.deleteNote(id);
        return Result.success("已删除");
    }

    @PutMapping("/notes/{id}/public")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public Result<Void> togglePublic(@PathVariable Long id) {
        knowledgeService.togglePublic(id);
        return Result.success("切换成功");
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
        return Result.success("已标为已读");
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notifService.markAllRead(uid());
        return Result.success("全部已读");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN')")
    public Result<Void> send(@RequestBody Notification n) {
        n.setSenderId(uid());
        notifService.send(n);
        return Result.success("通知已发送");
    }
}
