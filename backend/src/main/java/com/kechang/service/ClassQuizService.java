package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kechang.common.BusinessException;
import com.kechang.dto.ClassQuizAnswerDTO;
import com.kechang.dto.ClassQuizCreateDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassQuizService {

    private final ClassQuizMapper quizMapper;
    private final ClassQuizQuestionMapper quizQuestionMapper;
    private final ClassQuizAnswerMapper answerMapper;
    private final QuestionMapper questionMapper;
    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;

    public List<Map<String, Object>> teacherList(Long teacherId, Long courseId) {
        LambdaQueryWrapper<ClassQuiz> wrapper = new LambdaQueryWrapper<ClassQuiz>()
                .eq(ClassQuiz::getCreateBy, teacherId)
                .eq(courseId != null, ClassQuiz::getCourseId, courseId)
                .orderByDesc(ClassQuiz::getStartTime);
        return quizMapper.selectList(wrapper).stream().map(this::toSummary).collect(Collectors.toList());
    }

    public List<Map<String, Object>> studentActiveList(Long studentId) {
        List<Long> courseIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getStudentId, studentId))
                .stream().map(CourseStudent::getCourseId).collect(Collectors.toList());
        if (courseIds.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        return quizMapper.selectList(new LambdaQueryWrapper<ClassQuiz>()
                        .in(ClassQuiz::getCourseId, courseIds)
                        .eq(ClassQuiz::getStatus, "ACTIVE")
                        .gt(ClassQuiz::getEndTime, now)
                        .orderByDesc(ClassQuiz::getStartTime))
                .stream()
                .map(quiz -> sanitizeForStudent(withMyAnswer(toDetail(quiz), quiz.getId(), studentId)))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(ClassQuizCreateDTO dto, Long teacherId) {
        List<Long> questionIds = new ArrayList<>();
        if (dto.getQuestionIds() != null) {
            questionIds.addAll(dto.getQuestionIds().stream().filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        } else if (dto.getQuestionId() != null) {
            questionIds.add(dto.getQuestionId());
        }
        if (dto.getCourseId() == null || questionIds.isEmpty()) {
            throw BusinessException.badRequest("请选择课程和试题");
        }
        Course course = courseMapper.selectById(dto.getCourseId());
        if (course == null) {
            throw BusinessException.notFound("课程不存在");
        }
        if (!Objects.equals(course.getTeacherId(), teacherId)) {
            throw BusinessException.forbidden("只能在自己的课程中发起随堂测验");
        }
        List<Question> selectedQuestions = new ArrayList<>();
        for (Long questionId : questionIds) {
            Question question = questionMapper.selectById(questionId);
            if (question == null) {
                throw BusinessException.notFound("试题不存在");
            }
            if (question.getCourseId() != null && !Objects.equals(question.getCourseId(), dto.getCourseId())) {
                throw BusinessException.badRequest("试题不属于当前课程");
            }
            selectedQuestions.add(question);
        }
        int duration = dto.getDurationSeconds() != null && dto.getDurationSeconds() > 0
                ? dto.getDurationSeconds() : 120;
        ClassQuiz quiz = new ClassQuiz();
        quiz.setCourseId(dto.getCourseId());
        quiz.setQuestionId(questionIds.get(0));
        quiz.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : defaultQuizTitle(selectedQuestions));
        quiz.setDurationSeconds(duration);
        quiz.setStatus("ACTIVE");
        quiz.setCreateBy(teacherId);
        quiz.setStartTime(LocalDateTime.now());
        quiz.setEndTime(quiz.getStartTime().plusSeconds(duration));
        quizMapper.insert(quiz);
        for (int i = 0; i < questionIds.size(); i++) {
            ClassQuizQuestion item = new ClassQuizQuestion();
            item.setQuizId(quiz.getId());
            item.setQuestionId(questionIds.get(i));
            item.setSortOrder(i + 1);
            quizQuestionMapper.insert(item);
        }
        return toDetail(quiz);
    }

    public Map<String, Object> detail(Long id, Long userId, String role) {
        ClassQuiz quiz = getQuiz(id);
        if ("ROLE_TEACHER".equals(role)) {
            Course course = courseMapper.selectById(quiz.getCourseId());
            if (course == null || !Objects.equals(course.getTeacherId(), userId)) {
                throw BusinessException.forbidden("无权查看该测验");
            }
            return toDetail(quiz);
        }
        checkStudentEnrolled(quiz.getCourseId(), userId);
        return sanitizeForStudent(withMyAnswer(toDetail(quiz), quiz.getId(), userId));
    }

    @Transactional
    public void close(Long id, Long teacherId) {
        ClassQuiz quiz = getQuiz(id);
        Course course = courseMapper.selectById(quiz.getCourseId());
        if (course == null || !Objects.equals(course.getTeacherId(), teacherId)) {
            throw BusinessException.forbidden("只能结束自己课程的测验");
        }
        quiz.setStatus("ENDED");
        quiz.setEndTime(LocalDateTime.now());
        quizMapper.updateById(quiz);
    }

    @Transactional
    public Map<String, Object> submit(Long id, ClassQuizAnswerDTO dto, Long studentId) {
        ClassQuiz quiz = getQuiz(id);
        checkStudentEnrolled(quiz.getCourseId(), studentId);
        if (!"ACTIVE".equals(quiz.getStatus()) || LocalDateTime.now().isAfter(quiz.getEndTime())) {
            throw BusinessException.badRequest("测验已结束，不能提交");
        }
        if (answerMapper.selectCount(new LambdaQueryWrapper<ClassQuizAnswer>()
                .eq(ClassQuizAnswer::getQuizId, id)
                .eq(ClassQuizAnswer::getStudentId, studentId)) > 0) {
            throw BusinessException.badRequest("你已经提交过该测验");
        }
        List<Question> questions = getQuizQuestions(quiz);
        Map<Long, Object> submitted = submittedAnswerMap(dto);
        for (Question question : questions) {
            Object rawAnswer = submitted.containsKey(question.getId()) ? submitted.get(question.getId()) : submitted.get(null);
            String answer = normalizeSubmittedAnswer(rawAnswer);
            if ("MULTI".equals(question.getQuestionType())) {
                answer = String.join(",", normalizeMulti(answer));
            }
            ClassQuizAnswer record = new ClassQuizAnswer();
            record.setQuizId(id);
            record.setQuestionId(question.getId());
            record.setStudentId(studentId);
            record.setAnswer(answer);
            record.setIsCorrect(isAnswerCorrect(question, answer) ? 1 : 0);
            record.setSubmitTime(LocalDateTime.now());
            answerMapper.insert(record);
        }
        return sanitizeForStudent(withMyAnswer(toDetail(quiz), quiz.getId(), studentId));
    }

    public Map<String, Object> stats(Long id, Long userId, String role) {
        ClassQuiz quiz = getQuiz(id);
        if ("ROLE_TEACHER".equals(role)) {
            Course course = courseMapper.selectById(quiz.getCourseId());
            if (course == null || !Objects.equals(course.getTeacherId(), userId)) {
                throw BusinessException.forbidden("无权查看该测验统计");
            }
        } else {
            checkStudentEnrolled(quiz.getCourseId(), userId);
        }
        Map<String, Object> detail = toDetail(quiz);
        List<ClassQuizAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<ClassQuizAnswer>()
                .eq(ClassQuizAnswer::getQuizId, id));
        List<Question> questions = getQuizQuestions(quiz);
        long questionCount = Math.max(1, questions.size());
        long totalStudents = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, quiz.getCourseId()));
        long correctCount = answers.stream().filter(a -> Objects.equals(a.getIsCorrect(), 1)).count();
        List<Map<String, Object>> questionStats = questions.stream()
                .map(q -> questionDistribution(q, answers.stream()
                        .filter(a -> Objects.equals(a.getQuestionId(), q.getId()))
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());
        long submittedStudents = answers.stream().map(ClassQuizAnswer::getStudentId).distinct().count();
        detail.put("totalStudents", totalStudents);
        detail.put("submittedCount", submittedStudents);
        detail.put("unsubmittedCount", Math.max(0, totalStudents - submittedStudents));
        detail.put("correctCount", correctCount);
        detail.put("accuracy", answers.isEmpty() ? null : Math.round(correctCount * 1000.0 / answers.size()) / 10.0);
        detail.put("questionCount", questionCount);
        detail.put("questionStats", questionStats);
        detail.put("distribution", questionStats.isEmpty() ? List.of() : questionStats.get(0).get("distribution"));
        if (!"ROLE_TEACHER".equals(role)) {
            sanitizeForStudent(detail);
        }
        return detail;
    }

    private Map<String, Object> toSummary(ClassQuiz quiz) {
        Map<String, Object> map = toDetail(quiz);
        map.putAll(statsLite(quiz));
        return map;
    }

    private Map<String, Object> statsLite(ClassQuiz quiz) {
        long submitted = answerMapper.selectList(new LambdaQueryWrapper<ClassQuizAnswer>()
                .eq(ClassQuizAnswer::getQuizId, quiz.getId()))
                .stream().map(ClassQuizAnswer::getStudentId).distinct().count();
        long total = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, quiz.getCourseId()));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("submittedCount", submitted);
        map.put("totalStudents", total);
        return map;
    }

    private String defaultQuizTitle(List<Question> questions) {
        if (questions.size() == 1) {
            String content = questions.get(0).getContent();
            return content == null ? "随堂测验" : content.substring(0, Math.min(content.length(), 40));
        }
        return "随堂测验（" + questions.size() + "题）";
    }

    private List<Question> getQuizQuestions(ClassQuiz quiz) {
        List<ClassQuizQuestion> items = quizQuestionMapper.selectList(new LambdaQueryWrapper<ClassQuizQuestion>()
                .eq(ClassQuizQuestion::getQuizId, quiz.getId())
                .orderByAsc(ClassQuizQuestion::getSortOrder));
        if (items.isEmpty() && quiz.getQuestionId() != null) {
            Question legacy = questionMapper.selectById(quiz.getQuestionId());
            return legacy == null ? List.of() : List.of(legacy);
        }
        List<Question> questions = new ArrayList<>();
        for (ClassQuizQuestion item : items) {
            Question question = questionMapper.selectById(item.getQuestionId());
            if (question != null) {
                questions.add(question);
            }
        }
        return questions;
    }

    private Map<Long, Object> submittedAnswerMap(ClassQuizAnswerDTO dto) {
        Map<Long, Object> map = new LinkedHashMap<>();
        if (dto == null) {
            return map;
        }
        if (dto.getAnswers() != null) {
            for (ClassQuizAnswerDTO.AnswerItem item : dto.getAnswers()) {
                if (item.getQuestionId() != null) {
                    map.put(item.getQuestionId(), item.getAnswer());
                }
            }
        }
        if (map.isEmpty() && dto.getAnswer() != null) {
            map.put(null, dto.getAnswer());
        }
        return map;
    }

    private Map<String, Object> questionDistribution(Question question, List<ClassQuizAnswer> answers) {
        Map<String, Long> counts = answers.stream()
                .collect(Collectors.groupingBy(a -> StringUtils.hasText(a.getAnswer()) ? a.getAnswer() : "未作答",
                        LinkedHashMap::new, Collectors.counting()));
        List<Map<String, Object>> distribution = counts.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("answer", e.getKey());
                    item.put("count", e.getValue());
                    item.put("percent", answers.isEmpty() ? 0 : Math.round(e.getValue() * 1000.0 / answers.size()) / 10.0);
                    return item;
                })
                .collect(Collectors.toList());
        long correct = answers.stream().filter(a -> Objects.equals(a.getIsCorrect(), 1)).count();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("questionId", question.getId());
        item.put("question", question);
        item.put("submittedCount", answers.size());
        item.put("correctCount", correct);
        item.put("accuracy", answers.isEmpty() ? null : Math.round(correct * 1000.0 / answers.size()) / 10.0);
        item.put("distribution", distribution);
        return item;
    }

    private Map<String, Object> toDetail(ClassQuiz quiz) {
        refreshEndedStatus(quiz);
        Course course = courseMapper.selectById(quiz.getCourseId());
        List<Question> questions = getQuizQuestions(quiz);
        Question question = questions.isEmpty() ? questionMapper.selectById(quiz.getQuestionId()) : questions.get(0);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", quiz.getId());
        map.put("courseId", quiz.getCourseId());
        map.put("courseName", course != null ? course.getCourseName() : null);
        map.put("questionId", quiz.getQuestionId());
        map.put("title", quiz.getTitle());
        map.put("durationSeconds", quiz.getDurationSeconds());
        map.put("status", quiz.getStatus());
        map.put("startTime", quiz.getStartTime());
        map.put("endTime", quiz.getEndTime());
        map.put("remainingSeconds", Math.max(0, java.time.Duration.between(LocalDateTime.now(), quiz.getEndTime()).toSeconds()));
        map.put("question", question);
        map.put("questions", questions);
        map.put("questionCount", questions.size());
        return map;
    }

    private Map<String, Object> withMyAnswer(Map<String, Object> map, Long quizId, Long studentId) {
        List<ClassQuizAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<ClassQuizAnswer>()
                .eq(ClassQuizAnswer::getQuizId, quizId)
                .eq(ClassQuizAnswer::getStudentId, studentId));
        Map<Long, Object> myAnswers = new LinkedHashMap<>();
        Map<Long, Object> myCorrectMap = new LinkedHashMap<>();
        for (ClassQuizAnswer answer : answers) {
            myAnswers.put(answer.getQuestionId(), answer.getAnswer());
            myCorrectMap.put(answer.getQuestionId(), answer.getIsCorrect());
        }
        map.put("submitted", !answers.isEmpty());
        map.put("myAnswers", myAnswers);
        map.put("myCorrectMap", myCorrectMap);
        if (!answers.isEmpty()) {
            map.put("myAnswer", answers.get(0).getAnswer());
            map.put("myCorrect", answers.get(0).getIsCorrect());
        }
        return map;
    }

    private Map<String, Object> sanitizeForStudent(Map<String, Object> map) {
        Object raw = map.get("question");
        if (raw instanceof Question question) {
            map.put("question", safeQuestion(question));
        }
        Object rawList = map.get("questions");
        if (rawList instanceof Collection<?> list) {
            map.put("questions", list.stream()
                    .filter(Question.class::isInstance)
                    .map(Question.class::cast)
                    .map(this::safeQuestion)
                    .collect(Collectors.toList()));
        }
        Object statList = map.get("questionStats");
        if (statList instanceof Collection<?> list) {
            List<Object> safeStats = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> rawMap) {
                    Map<String, Object> safeMap = new LinkedHashMap<>();
                    rawMap.forEach((key, value) -> safeMap.put(String.valueOf(key), value));
                    Object question = safeMap.get("question");
                    if (question instanceof Question q) {
                        safeMap.put("question", safeQuestion(q));
                    }
                    safeStats.add(safeMap);
                }
            }
            map.put("questionStats", safeStats);
        }
        return map;
    }

    private Map<String, Object> safeQuestion(Question question) {
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("id", question.getId());
        safe.put("questionType", question.getQuestionType());
        safe.put("content", question.getContent());
        safe.put("options", question.getOptions());
        safe.put("score", question.getScore());
        safe.put("difficulty", question.getDifficulty());
        safe.put("knowledgePoint", question.getKnowledgePoint());
        safe.put("courseId", question.getCourseId());
        return safe;
    }

    private ClassQuiz getQuiz(Long id) {
        ClassQuiz quiz = quizMapper.selectById(id);
        if (quiz == null) {
            throw BusinessException.notFound("随堂测验不存在");
        }
        return quiz;
    }

    private void refreshEndedStatus(ClassQuiz quiz) {
        if ("ACTIVE".equals(quiz.getStatus()) && quiz.getEndTime() != null && LocalDateTime.now().isAfter(quiz.getEndTime())) {
            quiz.setStatus("ENDED");
            quizMapper.updateById(quiz);
        }
    }

    private void checkStudentEnrolled(Long courseId, Long studentId) {
        if (courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId)) == 0) {
            throw BusinessException.forbidden("请先加入课程后再参加随堂测验");
        }
    }

    private boolean isAnswerCorrect(Question question, String studentAnswer) {
        if (question == null || question.getAnswer() == null) {
            return false;
        }
        if ("MULTI".equals(question.getQuestionType())) {
            return normalizeMulti(question.getAnswer()).equals(normalizeMulti(studentAnswer));
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
        return Arrays.stream(text.split(","))
                .map(item -> item.replace("\"", "").replace("'", "").trim())
                .filter(StringUtils::hasText)
                .sorted()
                .collect(Collectors.toList());
    }
}
