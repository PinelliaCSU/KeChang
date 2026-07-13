package com.kechang.controller.exam;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.dto.AiGenerateRequest;
import com.kechang.dto.AiPaperResult;
import com.kechang.dto.ExamSubmitDTO;
import com.kechang.dto.PaperCreateDTO;
import com.kechang.entity.Exam;
import com.kechang.entity.ExamPaper;
import com.kechang.entity.ExamRecord;
import com.kechang.entity.Question;
import com.kechang.service.ExamService;
import com.kechang.vo.ExamListVO;
import com.kechang.vo.ExamRecordAnswerVO;
import com.kechang.vo.ExamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    // ==================== 试题库 ====================
    @GetMapping("/api/questions")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<PageResult<Question>> listQuestions(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String knowledgePoint,
            @RequestParam(required = false) Long courseId) {
        size = PageResult.clampSize(size);
        return Result.success(examService.getQuestionList(page, size, questionType, difficulty, knowledgePoint, courseId));
    }

    @PostMapping("/api/questions")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> createQuestion(@RequestBody Question question) {
        question.setCreateBy(getUserId());
        examService.createQuestion(question);
        return Result.success();
    }

    @PutMapping("/api/questions/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> updateQuestion(@PathVariable Long id, @RequestBody Question question) {
        examService.updateQuestion(id, question);
        return Result.success();
    }

    @DeleteMapping("/api/questions/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        examService.deleteQuestion(id);
        return Result.success();
    }

    // ==================== 试卷 ====================
    @GetMapping("/api/papers")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<PageResult<ExamPaper>> listPapers(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId) {
        size = PageResult.clampSize(size);
        return Result.success(examService.getPaperList(page, size, courseId));
    }

    @PostMapping("/api/papers")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> createPaper(@RequestBody PaperCreateDTO dto) {
        examService.createPaper(dto, getUserId());
        return Result.success();
    }

    @PostMapping("/api/papers/ai-generate")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<AiPaperResult> aiGenerate(@RequestBody AiGenerateRequest req) {
        return Result.success(examService.aiGeneratePaper(req));
    }

    @GetMapping("/api/papers/{id}")
    public Result<ExamVO> getPaper(@PathVariable Long id) {
        return Result.success(examService.getPaperDetail(id));
    }

    @DeleteMapping("/api/papers/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> deletePaper(@PathVariable Long id) {
        examService.deletePaper(id);
        return Result.success();
    }

    // ==================== 考试安排 ====================
    @GetMapping("/api/exams")
    public Result<PageResult<ExamListVO>> listExams(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId) {
        size = PageResult.clampSize(size);
        return Result.success(examService.getExamList(page, size, courseId, getUserId(), currentRole()));
    }

    @PostMapping("/api/exams")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> createExam(@RequestBody Exam exam) {
        examService.createExam(exam, getUserId());
        return Result.success();
    }

    @PutMapping("/api/exams/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> updateExam(@PathVariable Long id, @RequestBody Exam exam) {
        examService.updateExam(id, exam, getUserId());
        return Result.success();
    }

    @DeleteMapping("/api/exams/{id}")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id, getUserId());
        return Result.success();
    }

    @GetMapping("/api/exams/{id}")
    public Result<ExamVO> getExam(@PathVariable Long id) {
        return Result.success(examService.getExamDetail(id, "ROLE_TEACHER".equals(currentRole())));
    }

    // ==================== 考试作答 ====================
    @PostMapping("/api/exams/{id}/start")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<ExamVO> startExam(@PathVariable Long id) {
        return Result.success(examService.startExam(id, getUserId()));
    }

    @PostMapping("/api/exams/{id}/submit")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public Result<Void> submitExam(@PathVariable Long id, @RequestBody ExamSubmitDTO dto) {
        examService.submitExam(id, getUserId(), dto);
        return Result.success();
    }

    @GetMapping("/api/exams/{id}/records")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<List<ExamRecord>> getExamRecords(@PathVariable Long id) {
        return Result.success(examService.getExamRecords(id, getUserId()));
    }

    @GetMapping("/api/exams/records/{recordId}")
    public Result<List<ExamRecordAnswerVO>> getRecordDetail(@PathVariable Long recordId) {
        return Result.success(examService.getRecordDetail(recordId, getUserId(), currentRole()));
    }

    @PutMapping("/api/exams/records/{recordId}/mark")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> markSubjective(@PathVariable Long recordId,
            @RequestParam Long questionId,
            @RequestParam Double score) {
        examService.markSubjective(recordId, questionId, score, getUserId());
        return Result.success();
    }

    @PutMapping("/api/exams/records/{recordId}/mark-all")
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    public Result<Void> markSubjectiveBatch(@PathVariable Long recordId,
            @RequestBody Map<String, Double> scores) {
        examService.markSubjectiveBatch(recordId, scores, getUserId());
        return Result.success();
    }
}
