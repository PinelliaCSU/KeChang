package com.kechang.controller.exam;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.dto.ExamSubmitDTO;
import com.kechang.dto.PaperCreateDTO;
import com.kechang.entity.Exam;
import com.kechang.entity.ExamRecord;
import com.kechang.entity.ExamRecordAnswer;
import com.kechang.entity.Question;
import com.kechang.service.ExamService;
import com.kechang.vo.ExamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    private Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ==================== 试题库 ====================
    @GetMapping("/api/questions")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<PageResult<Question>> listQuestions(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String knowledgePoint,
            @RequestParam(required = false) Long courseId) {
        return Result.success(examService.getQuestionList(page, size, questionType, difficulty, knowledgePoint, courseId));
    }

    @PostMapping("/api/questions")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> createQuestion(@RequestBody Question question) {
        question.setCreateBy(getUserId());
        examService.createQuestion(question);
        return Result.success("试题添加成功");
    }

    @PutMapping("/api/questions/{id}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> updateQuestion(@PathVariable Long id, @RequestBody Question question) {
        examService.updateQuestion(id, question);
        return Result.success("试题更新成功");
    }

    @DeleteMapping("/api/questions/{id}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        examService.deleteQuestion(id);
        return Result.success("试题已删除");
    }

    // ==================== 试卷 ====================
    @GetMapping("/api/papers")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<PageResult<Exam>> listPapers(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId) {
        return Result.success(examService.getPaperList(page, size, courseId));
    }

    @PostMapping("/api/papers")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> createPaper(@RequestBody PaperCreateDTO dto) {
        examService.createPaper(dto, getUserId());
        return Result.success("试卷创建成功");
    }

    @PostMapping("/api/papers/ai-generate")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Map<String, Object>> aiGenerate(@RequestParam(required = false) String knowledgePoints,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String types) {
        return Result.success(examService.aiGeneratePaper(knowledgePoints, difficulty, types));
    }

    @GetMapping("/api/papers/{id}")
    public Result<ExamVO> getPaper(@PathVariable Long id) {
        return Result.success(examService.getPaperDetail(id));
    }

    @DeleteMapping("/api/papers/{id}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> deletePaper(@PathVariable Long id) {
        examService.deletePaper(id);
        return Result.success("试卷已删除");
    }

    // ==================== 考试安排 ====================
    @GetMapping("/api/exams")
    public Result<PageResult<Exam>> listExams(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId) {
        return Result.success(examService.getExamList(page, size, courseId));
    }

    @PostMapping("/api/exams")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> createExam(@RequestBody Exam exam) {
        examService.createExam(exam);
        return Result.success("考试创建成功");
    }

    @PutMapping("/api/exams/{id}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> updateExam(@PathVariable Long id, @RequestBody Exam exam) {
        examService.updateExam(id, exam);
        return Result.success("考试更新成功");
    }

    @GetMapping("/api/exams/{id}")
    public Result<ExamVO> getExam(@PathVariable Long id) {
        // 简化：根据请求上下文判断是否为教师
        return Result.success(examService.getExamDetail(id, true));
    }

    // ==================== 考试作答 ====================
    @PostMapping("/api/exams/{id}/start")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public Result<ExamVO> startExam(@PathVariable Long id) {
        return Result.success(examService.startExam(id, getUserId()));
    }

    @PostMapping("/api/exams/{id}/submit")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public Result<Void> submitExam(@PathVariable Long id, @RequestBody ExamSubmitDTO dto) {
        examService.submitExam(id, getUserId(), dto);
        return Result.success("交卷成功");
    }

    @GetMapping("/api/exams/{id}/records")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<List<ExamRecord>> getExamRecords(@PathVariable Long id) {
        return Result.success(examService.getExamRecords(id));
    }

    @GetMapping("/api/exams/records/{recordId}")
    public Result<List<ExamRecordAnswer>> getRecordDetail(@PathVariable Long recordId) {
        return Result.success(examService.getRecordDetail(recordId));
    }

    @PutMapping("/api/exams/records/{recordId}/mark")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<Void> markSubjective(@PathVariable Long recordId,
            @RequestParam Long questionId,
            @RequestParam Double score) {
        examService.markSubjective(recordId, questionId, score, getUserId());
        return Result.success("批阅成功");
    }
}
