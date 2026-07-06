package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.dto.ExamSubmitDTO;
import com.kechang.dto.PaperCreateDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.vo.ExamVO;
import com.kechang.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 试题考试服务
 */
@Service
@RequiredArgsConstructor
public class ExamService {

    private final QuestionMapper questionMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final ExamMapper examMapper;
    private final ExamRecordMapper recordMapper;
    private final ExamRecordAnswerMapper answerMapper;
    private final SysUserMapper userMapper;

    // ==================== 试题库 ====================
    public PageResult<Question> getQuestionList(int page, int size, String questionType,
            Integer difficulty, String knowledgePoint, Long courseId) {
        LambdaQueryWrapper<Question> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(questionType)) {
            w.eq(Question::getQuestionType, questionType);
        }
        if (difficulty != null) {
            w.eq(Question::getDifficulty, difficulty);
        }
        if (StringUtils.hasText(knowledgePoint)) {
            w.like(Question::getKnowledgePoint, knowledgePoint);
        }
        if (courseId != null) {
            w.eq(Question::getCourseId, courseId);
        }
        w.orderByDesc(Question::getCreateTime);
        Page<Question> p = questionMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void createQuestion(Question q) {
        questionMapper.insert(q);
    }

    public void updateQuestion(Long id, Question q) {
        q.setId(id);
        questionMapper.updateById(q);
    }

    public void deleteQuestion(Long id) {
        questionMapper.deleteById(id);
    }

    // ==================== 试卷 ====================
    public PageResult<ExamPaper> getPaperList(int page, int size, Long courseId) {
        LambdaQueryWrapper<ExamPaper> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(ExamPaper::getCourseId, courseId);
        }
        w.orderByDesc(ExamPaper::getCreateTime);
        Page<ExamPaper> p = paperMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    @Transactional
    public void createPaper(PaperCreateDTO dto, Long teacherId) {
        ExamPaper paper = new ExamPaper();
        paper.setPaperName(dto.getPaperName());
        paper.setCourseId(dto.getCourseId());
        paper.setTotalScore(dto.getTotalScore() != null ? dto.getTotalScore() : 100.0);
        paper.setDuration(dto.getDuration() != null ? dto.getDuration() : 120);
        paper.setPassScore(dto.getPassScore() != null ? dto.getPassScore() : 60.0);
        paper.setStatus("已发布");
        paper.setCreateBy(teacherId);
        paperMapper.insert(paper);
        // 关联题目
        if (dto.getQuestions() != null) {
            for (PaperCreateDTO.PaperQuestionItem item : dto.getQuestions()) {
                ExamPaperQuestion pq = new ExamPaperQuestion();
                pq.setPaperId(paper.getId());
                pq.setQuestionId(item.getQuestionId());
                pq.setQuestionScore(item.getQuestionScore());
                pq.setSortOrder(item.getSortOrder());
                paperQuestionMapper.insert(pq);
            }
        }
    }

    /**
     * AI 智能组卷（模拟）
     */
    public Map<String, Object> aiGeneratePaper(String knowledgePoints, String difficulty, String types) {
        // 模拟 AI 组卷：根据条件查询题库，随机选取
        List<Question> all = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .like(StringUtils.hasText(knowledgePoints), Question::getKnowledgePoint, knowledgePoints));
        Map<String, Object> result = new HashMap<>();
        result.put("totalQuestions", all.size());
        result.put("message", "AI 组卷功能基于知识库检索，共匹配 " + all.size() + " 道题目，建议选择其中合适题目手动组卷");
        result.put("suggestedQuestions", all.stream().limit(20).collect(Collectors.toList()));
        return result;
    }

    public ExamVO getPaperDetail(Long paperId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw BusinessException.notFound("试卷不存在");
        }
        ExamVO vo = new ExamVO();
        vo.setPaperId(paper.getId());
        vo.setPaperName(paper.getPaperName());
        vo.setTotalScore(paper.getTotalScore());
        vo.setDuration(paper.getDuration());
        vo.setPassScore(paper.getPassScore());
        // 获取试卷题目
        List<ExamPaperQuestion> pqs = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, paperId)
                        .orderByAsc(ExamPaperQuestion::getSortOrder));
        List<QuestionVO> questions = new ArrayList<>();
        for (ExamPaperQuestion pq : pqs) {
            Question q = questionMapper.selectById(pq.getQuestionId());
            if (q != null) {
                QuestionVO qv = new QuestionVO();
                qv.setId(q.getId());
                qv.setQuestionType(q.getQuestionType());
                qv.setContent(q.getContent());
                qv.setOptions(q.getOptions());
                qv.setAnswer(q.getAnswer());
                qv.setAnalysis(q.getAnalysis());
                qv.setScore(pq.getQuestionScore());
                qv.setDifficulty(q.getDifficulty());
                qv.setKnowledgePoint(q.getKnowledgePoint());
                questions.add(qv);
            }
        }
        vo.setQuestions(questions);
        return vo;
    }

    public void deletePaper(Long id) {
        paperMapper.deleteById(id);
    }

    // ==================== 考试安排 ====================
    public PageResult<Exam> getExamList(int page, int size, Long courseId) {
        LambdaQueryWrapper<Exam> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(Exam::getCourseId, courseId);
        }
        w.orderByDesc(Exam::getStartTime);
        Page<Exam> p = examMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void createExam(Exam exam) {
        examMapper.insert(exam);
    }

    public void updateExam(Long id, Exam exam) {
        exam.setId(id);
        examMapper.updateById(exam);
    }

    public ExamVO getExamDetail(Long examId, boolean isTeacher) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw BusinessException.notFound("考试不存在");
        }
        ExamVO vo = getPaperDetail(exam.getPaperId());
        vo.setId(exam.getId());
        vo.setExamName(exam.getExamName());
        vo.setCourseId(exam.getCourseId());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        vo.setDuration(exam.getDuration());
        vo.setStatus(exam.getStatus());
        vo.setAllowRetry(exam.getAllowRetry());
        if (!isTeacher) {
            // 学生端不返回答案
            if (vo.getQuestions() != null) {
                vo.getQuestions().forEach(q -> {
                    q.setAnswer(null);
                    q.setAnalysis(null);
                });
            }
        }
        return vo;
    }

    // ==================== 考试作答 ====================
    @Transactional
    public ExamVO startExam(Long examId, Long studentId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw BusinessException.notFound("考试不存在");
        }
        // 检查是否已有记录
        ExamRecord exist = recordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId).eq(ExamRecord::getStudentId, studentId));
        if (exist != null && exist.getIsSubmitted() == 1) {
            throw BusinessException.badRequest("您已完成该考试");
        }
        if (exist == null) {
            ExamRecord record = new ExamRecord();
            record.setExamId(examId);
            record.setStudentId(studentId);
            record.setStartTime(LocalDateTime.now());
            record.setIsSubmitted(0);
            record.setCheatCount(0);
            recordMapper.insert(record);
        }
        return getExamDetail(examId, false);
    }

    @Transactional
    public void submitExam(Long examId, Long studentId, ExamSubmitDTO dto) {
        ExamRecord record = recordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId).eq(ExamRecord::getStudentId, studentId));
        if (record == null) {
            throw BusinessException.badRequest("未找到考试记录");
        }
        if (record.getIsSubmitted() == 1) {
            throw BusinessException.badRequest("已提交过该考试");
        }

        record.setSubmitTime(LocalDateTime.now());
        record.setIsSubmitted(1);
        if (dto.getCheatCount() != null) {
            record.setCheatCount(dto.getCheatCount());
        }
        // 保存答案并自动判客观题
        double totalScore = 0;
        int objectiveTotal = 0;
        if (dto.getAnswers() != null) {
            for (ExamSubmitDTO.AnswerItem ans : dto.getAnswers()) {
                Question q = questionMapper.selectById(ans.getQuestionId());
                ExamRecordAnswer a = new ExamRecordAnswer();
                a.setRecordId(record.getId());
                a.setQuestionId(ans.getQuestionId());
                a.setStudentAnswer(ans.getAnswer());
                if (q != null && ("SINGLE".equals(q.getQuestionType()) || "MULTI".equals(q.getQuestionType())
                        || "JUDGE".equals(q.getQuestionType()))) {
                    boolean correct = q.getAnswer() != null && q.getAnswer().trim().equalsIgnoreCase(ans.getAnswer() != null ? ans.getAnswer().trim() : "");
                    a.setIsCorrect(correct ? 1 : 0);
                    if (correct) {
                        // 查试卷中的分值
                        ExamPaperQuestion pq = paperQuestionMapper.selectOne(new LambdaQueryWrapper<ExamPaperQuestion>()
                                .eq(ExamPaperQuestion::getQuestionId, ans.getQuestionId()));
                        double qScore = pq != null ? pq.getQuestionScore() : (q.getScore() != null ? q.getScore() : 0);
                        a.setScore(qScore);
                        totalScore += qScore;
                    } else {
                        a.setScore(0.0);
                    }
                }
                answerMapper.insert(a);
            }
        }
        record.setTotalScore(totalScore);
        // 判断及格
        Exam exam = examMapper.selectById(examId);
        ExamPaper paper = paperMapper.selectById(exam.getPaperId());
        double passScore = paper != null ? paper.getPassScore() : 60.0;
        record.setPassStatus(totalScore >= passScore ? 1 : 0);
        recordMapper.updateById(record);
    }

    /**
     * 教师查看考试所有记录
     */
    public List<ExamRecord> getExamRecords(Long examId) {
        return recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getExamId, examId));
    }

    /**
     * 答卷详情
     */
    public List<ExamRecordAnswer> getRecordDetail(Long recordId) {
        return answerMapper.selectList(new LambdaQueryWrapper<ExamRecordAnswer>().eq(ExamRecordAnswer::getRecordId, recordId));
    }

    /**
     * 教师批阅主观题
     */
    public void markSubjective(Long recordId, Long questionId, Double score, Long teacherId) {
        ExamRecordAnswer answer = answerMapper.selectOne(new LambdaQueryWrapper<ExamRecordAnswer>()
                .eq(ExamRecordAnswer::getRecordId, recordId)
                .eq(ExamRecordAnswer::getQuestionId, questionId));
        if (answer == null) {
            throw BusinessException.notFound("未找到答题记录");
        }
        answer.setScore(score);
        answer.setMarkedBy(teacherId);
        answerMapper.updateById(answer);
        // 更新总分
        Double total = answerMapper.selectList(new LambdaQueryWrapper<ExamRecordAnswer>()
                .eq(ExamRecordAnswer::getRecordId, recordId))
                .stream().mapToDouble(a -> a.getScore() != null ? a.getScore() : 0).sum();
        ExamRecord record = recordMapper.selectById(recordId);
        if (record != null) {
            record.setTotalScore(total);
            recordMapper.updateById(record);
        }
    }
}
