package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.dto.ExamSubmitDTO;
import com.kechang.dto.PaperCreateDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.vo.ExamListVO;
import com.kechang.vo.ExamRecordAnswerVO;
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
    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final GradeService gradeService;

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
        // 应用默认值
        double totalScore = dto.getTotalScore() != null ? dto.getTotalScore() : 100.0;
        int duration = dto.getDuration() != null ? dto.getDuration() : 120;
        double passScore = dto.getPassScore() != null ? dto.getPassScore() : 60.0;

        // 校验及格分不能大于总分
        if (passScore > totalScore) {
            throw BusinessException.badRequest("及格分不能大于总分");
        }

        // 校验题目分值合计，不一致时自动修正总分
        if (dto.getQuestions() != null && !dto.getQuestions().isEmpty()) {
            double questionSum = dto.getQuestions().stream()
                    .mapToDouble(PaperCreateDTO.PaperQuestionItem::getQuestionScore)
                    .sum();
            if (Math.abs(totalScore - questionSum) > 0.01) {
                totalScore = questionSum;
            }
        }

        ExamPaper paper = new ExamPaper();
        paper.setPaperName(dto.getPaperName());
        paper.setCourseId(dto.getCourseId());
        paper.setTotalScore(totalScore);
        paper.setDuration(duration);
        paper.setPassScore(passScore);
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
    public PageResult<ExamListVO> getExamList(int page, int size, Long courseId) {
        return getExamList(page, size, courseId, null, null);
    }

    public PageResult<ExamListVO> getExamList(int page, int size, Long courseId, Long currentUserId, String role) {
        LambdaQueryWrapper<Exam> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(Exam::getCourseId, courseId);
        }
        if ("ROLE_TEACHER".equals(role) && currentUserId != null) {
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                    .eq(Course::getTeacherId, currentUserId))
                    .stream().map(Course::getId).collect(Collectors.toList());
            if (courseIds.isEmpty()) {
                return PageResult.of(new ArrayList<>(), 0, page, size);
            }
            w.in(Exam::getCourseId, courseIds);
        }
        if ("ROLE_STUDENT".equals(role) && currentUserId != null) {
            List<Long> courseIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getStudentId, currentUserId))
                    .stream().map(CourseStudent::getCourseId).collect(Collectors.toList());
            if (courseIds.isEmpty()) {
                return PageResult.of(new ArrayList<>(), 0, page, size);
            }
            w.in(Exam::getCourseId, courseIds);
        }
        w.orderByDesc(Exam::getStartTime);
        Page<Exam> p = examMapper.selectPage(new Page<>(page, size), w);
        List<ExamListVO> vos = p.getRecords().stream().map(e -> {
            ExamListVO vo = new ExamListVO();
            vo.setId(e.getId());
            vo.setExamName(e.getExamName());
            vo.setPaperId(e.getPaperId());
            vo.setCourseId(e.getCourseId());
            vo.setStartTime(e.getStartTime());
            vo.setEndTime(e.getEndTime());
            vo.setDuration(e.getDuration());
            // 根据时间自动计算状态
            vo.setStatus(computeStatus(e.getStartTime(), e.getEndTime()));
            vo.setAllowRetry(e.getAllowRetry());
            vo.setCreateTime(e.getCreateTime());
            if ("ROLE_STUDENT".equals(role) && currentUserId != null) {
                ExamRecord record = recordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, e.getId())
                        .eq(ExamRecord::getStudentId, currentUserId));
                if (record != null) {
                    vo.setRecordId(record.getId());
                    vo.setSubmitted(record.getIsSubmitted());
                    vo.setTotalScore(record.getTotalScore());
                    vo.setPassStatus(record.getPassStatus());
                } else {
                    vo.setSubmitted(0);
                }
            }
            // 查询课程名称
            Course course = courseMapper.selectById(e.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getCourseName());
            }
            return vo;
        }).collect(Collectors.toList());
        return PageResult.of(vos, p.getTotal(), page, size);
    }

    public void createExam(Exam exam) {
        // 校验试卷存在
        ExamPaper paper = paperMapper.selectById(exam.getPaperId());
        if (paper == null) {
            throw BusinessException.notFound("关联试卷不存在");
        }
        // 校验课程一致性
        if (!paper.getCourseId().equals(exam.getCourseId())) {
            throw BusinessException.badRequest("试卷所属课程与考试所属课程不一致");
        }
        // 校验时间顺序
        if (exam.getStartTime() != null && exam.getEndTime() != null
                && !exam.getStartTime().isBefore(exam.getEndTime())) {
            throw BusinessException.badRequest("考试开始时间必须早于结束时间");
        }
        // 时长回退：未设置时从试卷继承
        if (exam.getDuration() == null || exam.getDuration() <= 0) {
            exam.setDuration(paper.getDuration());
        }
        examMapper.insert(exam);
    }

    public void updateExam(Long id, Exam exam) {
        // 校验试卷存在
        ExamPaper paper = paperMapper.selectById(exam.getPaperId());
        if (paper == null) {
            throw BusinessException.notFound("关联试卷不存在");
        }
        // 校验课程一致性
        if (!paper.getCourseId().equals(exam.getCourseId())) {
            throw BusinessException.badRequest("试卷所属课程与考试所属课程不一致");
        }
        // 校验时间顺序
        if (exam.getStartTime() != null && exam.getEndTime() != null
                && !exam.getStartTime().isBefore(exam.getEndTime())) {
            throw BusinessException.badRequest("考试开始时间必须早于结束时间");
        }
        // 时长回退：未设置时从试卷继承
        if (exam.getDuration() == null || exam.getDuration() <= 0) {
            exam.setDuration(paper.getDuration());
        }
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
        checkStudentEnrolled(exam.getCourseId(), studentId);
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw BusinessException.badRequest("考试尚未开始");
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw BusinessException.badRequest("考试已结束");
        }
        // 查询该学生考试记录（唯一约束 exam_id + student_id）
        ExamRecord exist = recordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId).eq(ExamRecord::getStudentId, studentId));
        if (exist != null && exist.getIsSubmitted() == 1) {
            // 不允许重考则拒绝
            if (exam.getAllowRetry() == null || exam.getAllowRetry() != 1) {
                throw BusinessException.badRequest("您已完成该考试");
            }
            // 允许重考：重置已有记录
            exist.setStartTime(LocalDateTime.now());
            exist.setSubmitTime(null);
            exist.setIsSubmitted(0);
            exist.setTotalScore(null);
            exist.setPassStatus(0);
            exist.setCheatCount(0);
            recordMapper.updateById(exist);
            // 删除旧的答题记录
            answerMapper.delete(new LambdaQueryWrapper<ExamRecordAnswer>()
                    .eq(ExamRecordAnswer::getRecordId, exist.getId()));
        } else if (exist == null) {
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
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw BusinessException.notFound("考试不存在");
        }
        checkStudentEnrolled(exam.getCourseId(), studentId);
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
        if (dto.getAnswers() != null) {
            for (ExamSubmitDTO.AnswerItem ans : dto.getAnswers()) {
                Question q = questionMapper.selectById(ans.getQuestionId());
                String studentAnswer = normalizeSubmittedAnswer(ans.getAnswer());
                ExamRecordAnswer a = new ExamRecordAnswer();
                a.setRecordId(record.getId());
                a.setQuestionId(ans.getQuestionId());
                a.setStudentAnswer(studentAnswer);
                if (q != null && ("SINGLE".equals(q.getQuestionType()) || "MULTI".equals(q.getQuestionType())
                        || "JUDGE".equals(q.getQuestionType()))) {
                    boolean correct = isAnswerCorrect(q, studentAnswer);
                    a.setIsCorrect(correct ? 1 : 0);
                    if (correct) {
                        // 查试卷中的分值
                        ExamPaperQuestion pq = paperQuestionMapper.selectOne(new LambdaQueryWrapper<ExamPaperQuestion>()
                                .eq(ExamPaperQuestion::getPaperId, exam.getPaperId())
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
        ExamPaper paper = paperMapper.selectById(exam.getPaperId());
        double passScore = paper != null ? paper.getPassScore() : 60.0;
        record.setPassStatus(totalScore >= passScore ? 1 : 0);
        recordMapper.updateById(record);
        gradeService.recalculateGrade(exam.getCourseId(), studentId);
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
    public List<ExamRecordAnswerVO> getRecordDetail(Long recordId) {
        ExamRecord record = recordMapper.selectById(recordId);
        Exam exam = record != null ? examMapper.selectById(record.getExamId()) : null;
        Long paperId = exam != null ? exam.getPaperId() : null;
        return answerMapper.selectList(new LambdaQueryWrapper<ExamRecordAnswer>().eq(ExamRecordAnswer::getRecordId, recordId))
                .stream().map(answer -> {
                    Question question = questionMapper.selectById(answer.getQuestionId());
                    ExamPaperQuestion paperQuestion = paperId == null ? null : paperQuestionMapper.selectOne(
                            new LambdaQueryWrapper<ExamPaperQuestion>()
                                    .eq(ExamPaperQuestion::getPaperId, paperId)
                                    .eq(ExamPaperQuestion::getQuestionId, answer.getQuestionId()));
                    ExamRecordAnswerVO vo = new ExamRecordAnswerVO();
                    vo.setId(answer.getId());
                    vo.setRecordId(answer.getRecordId());
                    vo.setQuestionId(answer.getQuestionId());
                    vo.setStudentAnswer(answer.getStudentAnswer());
                    vo.setIsCorrect(answer.getIsCorrect());
                    vo.setScore(answer.getScore());
                    vo.setMarkedBy(answer.getMarkedBy());
                    if (question != null) {
                        vo.setQuestionType(question.getQuestionType());
                        vo.setQuestionContent(question.getContent());
                        vo.setOptions(question.getOptions());
                        vo.setCorrectAnswer(question.getAnswer());
                        vo.setAnalysis(question.getAnalysis());
                        vo.setQuestionScore(paperQuestion != null ? paperQuestion.getQuestionScore() : question.getScore());
                    }
                    return vo;
                }).collect(Collectors.toList());
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
            Exam exam = examMapper.selectById(record.getExamId());
            ExamPaper paper = exam != null ? paperMapper.selectById(exam.getPaperId()) : null;
            double passScore = paper != null ? paper.getPassScore() : 60.0;
            record.setPassStatus(total >= passScore ? 1 : 0);
            recordMapper.updateById(record);
            if (exam != null) {
                gradeService.recalculateGrade(exam.getCourseId(), record.getStudentId());
            }
        }
    }

    public void deleteExam(Long id, Long teacherId) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw BusinessException.notFound("考试不存在");
        }
        Course course = courseMapper.selectById(exam.getCourseId());
        if (course != null && !course.getTeacherId().equals(teacherId)) {
            throw BusinessException.forbidden("只能删除自己课程的考试");
        }
        examMapper.deleteById(id);
    }

    private void checkStudentEnrolled(Long courseId, Long studentId) {
        if (courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId)) == 0) {
            throw BusinessException.forbidden("请先加入课程后再参加考试");
        }
    }

    private boolean isAnswerCorrect(Question question, String studentAnswer) {
        if (question == null || question.getAnswer() == null) {
            return false;
        }
        if ("MULTI".equals(question.getQuestionType())) {
            return normalizeMulti(question.getAnswer()).equals(normalizeMulti(studentAnswer));
        }
        return question.getAnswer().trim().equalsIgnoreCase(studentAnswer == null ? "" : studentAnswer.trim());
    }

    private String normalizeSubmittedAnswer(Object answer) {
        if (answer == null) {
            return "";
        }
        if (answer instanceof Collection<?> values) {
            return values.stream().map(this::normalizeSubmittedAnswer).filter(StringUtils::hasText).collect(Collectors.joining(","));
        }
        if (answer instanceof Map<?, ?> map) {
            Object label = map.get("label");
            if (label == null) {
                label = map.get("key");
            }
            if (label != null) {
                return String.valueOf(label).trim();
            }
            return map.values().stream().map(this::normalizeSubmittedAnswer).filter(StringUtils::hasText).collect(Collectors.joining(","));
        }
        return String.valueOf(answer).trim();
    }

    private List<String> normalizeMulti(String answer) {
        if (!StringUtils.hasText(answer)) {
            return new ArrayList<>();
        }
        String text = answer.trim();
        if (text.startsWith("[") && text.endsWith("]")) {
            text = text.substring(1, text.length() - 1);
        }
        List<String> values = Arrays.stream(text.split(","))
                .map(item -> item.replace("\"", "").replace("'", "").trim())
                .filter(StringUtils::hasText)
                .sorted()
                .collect(Collectors.toList());
        return values;
    }

    /**
     * 根据开始/结束时间自动计算考试状态
     */
    private String computeStatus(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "未开始";
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            return "未开始";
        } else if (now.isAfter(endTime)) {
            return "已结束";
        } else {
            return "进行中";
        }
    }
}
