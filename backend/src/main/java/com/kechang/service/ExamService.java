package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.dto.AiGenerateRequest;
import com.kechang.dto.AiPaperResult;
import com.kechang.dto.CodeRunResult;
import com.kechang.dto.ExamSubmitDTO;
import com.kechang.dto.PaperCreateDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.vo.ExamListVO;
import com.kechang.vo.ExamRecordAnswerVO;
import com.kechang.vo.ExamVO;
import com.kechang.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 试题考试服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;
    private final BailianApiService bailianApiService;
    private final KnowledgeService knowledgeService;
    private final CodeExecutionService codeExecutionService;

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
        operationLogService.record(teacherId, "考试管理", "创建试卷", "PAPER", paper.getId(), paper.getPaperName(),
                "创建考试试卷");

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
     * AI 智能组卷 — 调用阿里百炼 API 从题库中选题
     */
    public AiPaperResult aiGeneratePaper(AiGenerateRequest req) {
        // 课程 ID 必填
        if (req.getCourseId() == null) {
            throw BusinessException.badRequest("请选择所属课程");
        }

        // 查询候选题目
        LambdaQueryWrapper<Question> w = new LambdaQueryWrapper<>();
        w.eq(Question::getCourseId, req.getCourseId());
        if (StringUtils.hasText(req.getKnowledgePoints())) {
            w.like(Question::getKnowledgePoint, req.getKnowledgePoints());
        }
        if (req.getDifficulty() != null) {
            w.eq(Question::getDifficulty, req.getDifficulty());
        }
        if (!req.getTypeList().isEmpty()) {
            w.in(Question::getQuestionType, req.getTypeList());
        }

        List<Question> candidates = questionMapper.selectList(w);
        if (candidates.isEmpty()) {
            throw BusinessException.badRequest("题库中没有符合条件的题目，请调整筛选条件");
        }

        // 如果候选题目少于期望题数，给出提示但仍继续
        int expected = req.getQuestionCount() != null ? req.getQuestionCount() : 10;
        if (candidates.size() < expected) {
            // 候选不足，使用全部候选题目
            expected = candidates.size();
        }

        // 调用 AI 选题
        AiPaperResult result = bailianApiService.generatePaper(
                candidates,
                req.getKnowledgePoints(),
                req.getDifficulty(),
                req.getTypeList(),
                expected,
                req.getTotalScore()
        );

        // 如果用户指定了试卷名称则使用，否则用 AI 生成的
        if (StringUtils.hasText(req.getPaperName())) {
            result.setPaperName(req.getPaperName());
        }

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
                qv.setProgrammingConfig(q.getProgrammingConfig());
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
                    if (record.getIsSubmitted() != null && record.getIsSubmitted() == 1
                            && record.getTotalScore() != null
                            && "已结束".equals(vo.getStatus())) {
                        vo.setStatus("已出分");
                    }
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

    public void createExam(Exam exam, Long teacherId) {
        // 校验试卷存在
        ExamPaper paper = paperMapper.selectById(exam.getPaperId());
        if (paper == null) {
            throw BusinessException.notFound("关联试卷不存在");
        }
        // 校验课程一致性
        if (!paper.getCourseId().equals(exam.getCourseId())) {
            throw BusinessException.badRequest("试卷所属课程与考试所属课程不一致");
        }
        checkCourseOwner(exam.getCourseId(), teacherId);
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
        Course course = courseMapper.selectById(exam.getCourseId());
        notificationService.sendToCourseStudents(exam.getCourseId(), teacherId, "考试",
                "新考试：" + exam.getExamName(),
                "课程《" + (course != null ? course.getCourseName() : "课程") + "》发布了考试，请在规定时间内参加。");
        operationLogService.record(teacherId, "考试管理", "发布考试", "EXAM", exam.getId(), exam.getExamName(),
                "发布课程考试");
    }

    public void updateExam(Long id, Exam exam, Long teacherId) {
        Exam old = examMapper.selectById(id);
        if (old == null) {
            throw BusinessException.notFound("考试不存在");
        }
        checkCourseOwner(old.getCourseId(), teacherId);
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
        operationLogService.record(teacherId, "考试管理", "编辑考试", "EXAM", id, exam.getExamName(),
                "更新考试安排");
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
        vo.setStatus(computeStatus(exam.getStartTime(), exam.getEndTime()));
        vo.setAllowRetry(exam.getAllowRetry());
        if (!isTeacher) {
            // 学生端不返回答案，并移除编程题隐藏测试用例
            if (vo.getQuestions() != null) {
                vo.getQuestions().forEach(q -> {
                    q.setAnswer(null);
                    q.setAnalysis(null);
                    q.setProgrammingConfig(stripHiddenTestCases(q.getProgrammingConfig()));
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
                } else if (q != null && "CODE".equals(q.getQuestionType())) {
                    // 编程题：调用 Docker 沙箱判题
                    try {
                        String language = "cpp"; // 从 programming_config 中获取
                        if (q.getProgrammingConfig() != null) {
                            CodeExecutionService.ProgrammingConfig pc
                                    = OBJECT_MAPPER.readValue(q.getProgrammingConfig(),
                                            CodeExecutionService.ProgrammingConfig.class);
                            if (pc.getLanguage() != null) {
                                language = pc.getLanguage();
                            }
                        }
                        CodeRunResult result = codeExecutionService.judgeByQuestion(
                                language, studentAnswer, q.getId());
                        // 按通过用例比例计算得分
                        double qScore = 0;
                        ExamPaperQuestion pq = paperQuestionMapper.selectOne(new LambdaQueryWrapper<ExamPaperQuestion>()
                                .eq(ExamPaperQuestion::getPaperId, exam.getPaperId())
                                .eq(ExamPaperQuestion::getQuestionId, ans.getQuestionId()));
                        double maxScore = pq != null ? pq.getQuestionScore() : (q.getScore() != null ? q.getScore() : 0);
                        if (result.getTotalCases() > 0) {
                            qScore = maxScore * result.getPassedCases() / result.getTotalCases();
                            // 保留一位小数
                            qScore = Math.round(qScore * 10.0) / 10.0;
                        }
                        a.setScore(qScore);
                        a.setIsCorrect(result.getPassedCases() == result.getTotalCases() && result.getTotalCases() > 0 ? 1 : 0);
                        totalScore += qScore;
                    } catch (Exception e) {
                        log.warn("编程题判题异常 questionId={}: {}", q.getId(), e.getMessage());
                        a.setScore(0.0);
                        a.setIsCorrect(0);
                    }
                }
                answerMapper.insert(a);
                // 自动收集错题到知识库
                if (a.getIsCorrect() != null && a.getIsCorrect() == 0 && q != null) {
                    Course course = courseMapper.selectById(exam.getCourseId());
                    knowledgeService.collectWrongQuestion(studentId, q, exam.getCourseId(),
                            course != null ? course.getCourseName() : null, "EXAM", examId,
                            a.getStudentAnswer());
                }
            }
        }
        record.setTotalScore(totalScore);
        // 判断及格
        ExamPaper paper = paperMapper.selectById(exam.getPaperId());
        double passScore = paper != null ? paper.getPassScore() : 60.0;
        record.setPassStatus(totalScore >= passScore ? 1 : 0);
        recordMapper.updateById(record);
        gradeService.recalculateGrade(exam.getCourseId(), studentId);
        increaseCourseProgress(exam.getCourseId(), studentId, 15.0);
    }

    /**
     * 教师查看考试所有记录
     */
    public List<ExamRecord> getExamRecords(Long examId, Long teacherId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw BusinessException.notFound("考试不存在");
        }
        checkCourseOwner(exam.getCourseId(), teacherId);
        return recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getExamId, examId));
    }

    /**
     * 答卷详情
     */
    public List<ExamRecordAnswerVO> getRecordDetail(Long recordId, Long userId, String role) {
        ExamRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw BusinessException.notFound("考试记录不存在");
        }
        Exam exam = record != null ? examMapper.selectById(record.getExamId()) : null;
        if ("ROLE_STUDENT".equals(role) && !userId.equals(record.getStudentId())) {
            throw BusinessException.forbidden("只能查看自己的考试记录");
        }
        if ("ROLE_TEACHER".equals(role) && exam != null) {
            checkCourseOwner(exam.getCourseId(), userId);
        }
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
        ExamRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw BusinessException.notFound("考试记录不存在");
        }
        Exam exam = examMapper.selectById(record.getExamId());
        if (exam != null) {
            checkCourseOwner(exam.getCourseId(), teacherId);
        }
        answer.setScore(score);
        answer.setMarkedBy(teacherId);
        answerMapper.updateById(answer);
        // 更新总分
        Double total = answerMapper.selectList(new LambdaQueryWrapper<ExamRecordAnswer>()
                .eq(ExamRecordAnswer::getRecordId, recordId))
                .stream().mapToDouble(a -> a.getScore() != null ? a.getScore() : 0).sum();
        if (record != null) {
            record.setTotalScore(total);
            ExamPaper paper = exam != null ? paperMapper.selectById(exam.getPaperId()) : null;
            double passScore = paper != null ? paper.getPassScore() : 60.0;
            record.setPassStatus(total >= passScore ? 1 : 0);
            recordMapper.updateById(record);
            if (exam != null) {
                gradeService.recalculateGrade(exam.getCourseId(), record.getStudentId());
                notificationService.sendToUser(record.getStudentId(), teacherId, exam.getCourseId(), "考试",
                        "考试成绩更新：" + exam.getExamName(),
                        "你的考试《" + exam.getExamName() + "》主观题已批改，当前总分：" + total + "。");
                operationLogService.record(teacherId, "考试管理", "批改主观题", "EXAM_RECORD", record.getId(), exam.getExamName(),
                        "批改考试主观题并更新总分");
            }
        }
    }

    @Transactional
    public void markSubjectiveBatch(Long recordId, Map<String, Double> scores, Long teacherId) {
        if (scores == null || scores.isEmpty()) {
            throw BusinessException.badRequest("scores is empty");
        }
        ExamRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw BusinessException.notFound("exam record not found");
        }
        Exam exam = examMapper.selectById(record.getExamId());
        if (exam != null) {
            checkCourseOwner(exam.getCourseId(), teacherId);
        }

        List<ExamRecordAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<ExamRecordAnswer>()
                .eq(ExamRecordAnswer::getRecordId, recordId));
        for (ExamRecordAnswer answer : answers) {
            String questionId = String.valueOf(answer.getQuestionId());
            if (!scores.containsKey(questionId)) {
                continue;
            }
            Double score = scores.get(questionId);
            answer.setScore(score == null ? 0.0 : score);
            answer.setMarkedBy(teacherId);
            answerMapper.updateById(answer);
        }

        Double total = answerMapper.selectList(new LambdaQueryWrapper<ExamRecordAnswer>()
                .eq(ExamRecordAnswer::getRecordId, recordId))
                .stream().mapToDouble(a -> a.getScore() != null ? a.getScore() : 0).sum();
        record.setTotalScore(total);
        ExamPaper paper = exam != null ? paperMapper.selectById(exam.getPaperId()) : null;
        double passScore = paper != null ? paper.getPassScore() : 60.0;
        record.setPassStatus(total >= passScore ? 1 : 0);
        recordMapper.updateById(record);
        if (exam != null) {
            gradeService.recalculateGrade(exam.getCourseId(), record.getStudentId());
            notificationService.sendToUser(record.getStudentId(), teacherId, exam.getCourseId(), "EXAM",
                    "Exam score updated: " + exam.getExamName(),
                    "Your exam score has been updated. Current total score: " + total);
            operationLogService.record(teacherId, "EXAM", "MARK_EXAM_RECORD", "EXAM_RECORD", record.getId(), exam.getExamName(),
                    "Batch marked subjective answers and updated total score");
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
        operationLogService.record(teacherId, "考试管理", "删除考试", "EXAM", exam.getId(), exam.getExamName(),
                "删除考试安排");
    }

    private void checkStudentEnrolled(Long courseId, Long studentId) {
        if (courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId)) == 0) {
            throw BusinessException.forbidden("请先加入课程后再参加考试");
        }
    }

    private void checkCourseOwner(Long courseId, Long teacherId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        if (!teacherId.equals(course.getTeacherId())) {
            throw BusinessException.forbidden("只能管理自己课程的考试");
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

    /**
     * 移除编程题配置中的隐藏测试用例（学生端安全处理）
     */
    private String stripHiddenTestCases(String programmingConfig) {
        if (programmingConfig == null || programmingConfig.isBlank()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = OBJECT_MAPPER.readTree(programmingConfig);
            if (root.has("hiddenTestCases")) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) root).remove("hiddenTestCases");
            }
            // 也移除 scoringMode 等不必要的字段（可选）
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAnswerCorrect(Question question, String studentAnswer) {
        if (question == null || question.getAnswer() == null) {
            return false;
        }
        if ("MULTI".equals(question.getQuestionType())) {
            return normalizeObjectiveAnswer(question, question.getAnswer()).equals(normalizeObjectiveAnswer(question, studentAnswer));
        }
        if ("SINGLE".equals(question.getQuestionType())) {
            return normalizeObjectiveAnswer(question, question.getAnswer()).equals(normalizeObjectiveAnswer(question, studentAnswer));
        }
        if ("JUDGE".equals(question.getQuestionType())) {
            return normalizeJudge(question.getAnswer()).equals(normalizeJudge(studentAnswer));
        }
        return question.getAnswer().trim().equalsIgnoreCase(studentAnswer == null ? "" : studentAnswer.trim());
    }

    private String normalizeJudge(String answer) {
        String text = answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
        if ("1".equals(text) || "对".equals(text) || "正确".equals(text) || "true".equals(text)) {
            return "1";
        }
        if ("0".equals(text) || "错".equals(text) || "错误".equals(text) || "false".equals(text)) {
            return "0";
        }
        return text;
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

    private List<String> normalizeObjectiveAnswer(Question question, String answer) {
        List<OptionItem> options = parseOptions(question.getOptions());
        return normalizeMulti(answer).stream()
                .map(item -> optionTokenToLabel(item, options))
                .filter(StringUtils::hasText)
                .sorted()
                .collect(Collectors.toList());
    }

    private String optionTokenToLabel(String token, List<OptionItem> options) {
        String text = token == null ? "" : token.trim();
        if (!StringUtils.hasText(text)) {
            return "";
        }
        for (OptionItem option : options) {
            if (option.label.equalsIgnoreCase(text) || option.content.equals(text)) {
                return option.label;
            }
        }
        return text.toUpperCase(Locale.ROOT);
    }

    private List<OptionItem> parseOptions(String optionsJson) {
        if (!StringUtils.hasText(optionsJson)) {
            return new ArrayList<>();
        }
        try {
            List<Object> raw = OBJECT_MAPPER.readValue(optionsJson, new TypeReference<List<Object>>() {
            });
            List<OptionItem> options = new ArrayList<>();
            for (int i = 0; i < raw.size(); i++) {
                Object item = raw.get(i);
                String fallbackLabel = String.valueOf((char) ('A' + i));
                if (item instanceof Map<?, ?> map) {
                    Object label = map.get("label") != null ? map.get("label") : map.get("key");
                    Object content = map.get("content") != null ? map.get("content") : map.get("value");
                    options.add(new OptionItem(label == null ? fallbackLabel : String.valueOf(label).trim(),
                            content == null ? "" : String.valueOf(content).trim()));
                } else {
                    options.add(new OptionItem(fallbackLabel, String.valueOf(item).trim()));
                }
            }
            return options;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private record OptionItem(String label, String content) {

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
