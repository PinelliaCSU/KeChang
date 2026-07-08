package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.dto.SurveyCreateDTO;
import com.kechang.dto.SurveySubmitDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.vo.AdminSurveyStatsVO;
import com.kechang.vo.SurveyStatsVO;
import com.kechang.vo.SurveyVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 问卷服务
 */
@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyMapper surveyMapper;
    private final SurveyQuestionMapper questionMapper;
    private final SurveyResponseMapper responseMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper userMapper;
    private final CourseStudentMapper courseStudentMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 问卷 CRUD ====================
    /**
     * 问卷列表（教师看自己的，学生看已选课程+进行中的，管理员看全部）
     */
    public PageResult<SurveyVO> list(int page, int size, Long courseId, Long userId, String role) {
        LambdaQueryWrapper<Survey> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(Survey::getCourseId, courseId);
        }

        if ("ROLE_TEACHER".equals(role)) {
            w.eq(Survey::getCreateBy, userId);
        } else if ("ROLE_STUDENT".equals(role)) {
            // 学生只能看到自己已选课程中"进行中"的问卷
            List<CourseStudent> enrollments = courseStudentMapper.selectList(
                    new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getStudentId, userId));
            List<Long> enrolledCourseIds = enrollments.stream().map(CourseStudent::getCourseId).collect(Collectors.toList());
            if (enrolledCourseIds.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0, page, size);
            }
            w.in(Survey::getCourseId, enrolledCourseIds);
        }

        w.orderByDesc(Survey::getCreateTime);
        Page<Survey> p = surveyMapper.selectPage(new Page<>(page, size), w);

        List<SurveyVO> vos = p.getRecords().stream().map(s -> toSurveyVO(s, userId)).collect(Collectors.toList());
        return PageResult.of(vos, p.getTotal(), page, size);
    }

    /**
     * 创建问卷（含题目），事务
     */
    @Transactional
    public void create(SurveyCreateDTO dto, Long userId) {
        // 1. 创建问卷
        Survey s = new Survey();
        s.setTitle(dto.getTitle());
        s.setDescription(dto.getDescription());
        s.setCourseId(dto.getCourseId());
        s.setStartDate(dto.getStartDate());
        s.setEndDate(dto.getEndDate());
        s.setStatus(dto.getStatus() != null ? dto.getStatus() : "未开始");
        s.setCreateBy(userId);
        surveyMapper.insert(s);

        // 2. 批量创建题目
        if (dto.getQuestions() != null && !dto.getQuestions().isEmpty()) {
            for (SurveyCreateDTO.QuestionItem qi : dto.getQuestions()) {
                SurveyQuestion q = new SurveyQuestion();
                q.setSurveyId(s.getId());
                q.setQuestionContent(qi.getQuestionContent());
                q.setQuestionType(qi.getQuestionType());
                q.setOptions(qi.getOptions());
                q.setSortOrder(qi.getSortOrder() != null ? qi.getSortOrder() : 0);
                q.setIsRequired(qi.getIsRequired() != null ? qi.getIsRequired() : 1);
                questionMapper.insert(q);
            }
        }
    }

    /**
     * 更新问卷基本信息
     */
    public void update(Long surveyId, SurveyCreateDTO dto, Long userId) {
        Survey s = surveyMapper.selectById(surveyId);
        if (s == null) {
            throw BusinessException.notFound("问卷不存在");
        }
        if (!s.getCreateBy().equals(userId)) {
            throw BusinessException.forbidden("无权修改该问卷");
        }

        if (dto.getTitle() != null) {
            s.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            s.setDescription(dto.getDescription());
        }
        if (dto.getCourseId() != null) {
            s.setCourseId(dto.getCourseId());
        }
        if (dto.getStartDate() != null) {
            s.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            s.setEndDate(dto.getEndDate());
        }
        if (dto.getStatus() != null) {
            s.setStatus(dto.getStatus());
        }
        surveyMapper.updateById(s);

        // 如果带题目，则全量替换题目
        if (dto.getQuestions() != null && !dto.getQuestions().isEmpty()) {
            // 删除旧题目
            questionMapper.delete(new LambdaQueryWrapper<SurveyQuestion>().eq(SurveyQuestion::getSurveyId, surveyId));
            // 批量插入新题目
            for (SurveyCreateDTO.QuestionItem qi : dto.getQuestions()) {
                SurveyQuestion q = new SurveyQuestion();
                q.setSurveyId(surveyId);
                q.setQuestionContent(qi.getQuestionContent());
                q.setQuestionType(qi.getQuestionType());
                q.setOptions(qi.getOptions());
                q.setSortOrder(qi.getSortOrder() != null ? qi.getSortOrder() : 0);
                q.setIsRequired(qi.getIsRequired() != null ? qi.getIsRequired() : 1);
                questionMapper.insert(q);
            }
        }
    }

    /**
     * 删除问卷（级联删除题目和回答）
     */
    @Transactional
    public void delete(Long surveyId, Long userId) {
        Survey s = surveyMapper.selectById(surveyId);
        if (s == null) {
            throw BusinessException.notFound("问卷不存在");
        }
        if (!s.getCreateBy().equals(userId)) {
            throw BusinessException.forbidden("无权删除该问卷");
        }

        questionMapper.delete(new LambdaQueryWrapper<SurveyQuestion>().eq(SurveyQuestion::getSurveyId, surveyId));
        responseMapper.delete(new LambdaQueryWrapper<SurveyResponse>().eq(SurveyResponse::getSurveyId, surveyId));
        surveyMapper.deleteById(surveyId);
    }

    /**
     * 获取问卷详情（含题目列表）
     */
    public SurveyVO getDetail(Long surveyId, Long userId) {
        Survey s = surveyMapper.selectById(surveyId);
        if (s == null) {
            throw BusinessException.notFound("问卷不存在");
        }
        return toSurveyVO(s, userId);
    }

    // ==================== 题目管理 ====================
    /**
     * 添加题目
     */
    public void addQuestion(Long surveyId, SurveyQuestion question, Long userId) {
        Survey s = surveyMapper.selectById(surveyId);
        if (s == null) {
            throw BusinessException.notFound("问卷不存在");
        }
        if (!s.getCreateBy().equals(userId)) {
            throw BusinessException.forbidden("无权操作");
        }

        question.setSurveyId(surveyId);
        questionMapper.insert(question);
    }

    /**
     * 更新题目
     */
    public void updateQuestion(Long surveyId, Long questionId, SurveyQuestion question, Long userId) {
        Survey s = surveyMapper.selectById(surveyId);
        if (s == null) {
            throw BusinessException.notFound("问卷不存在");
        }
        if (!s.getCreateBy().equals(userId)) {
            throw BusinessException.forbidden("无权操作");
        }

        SurveyQuestion existing = questionMapper.selectById(questionId);
        if (existing == null || !existing.getSurveyId().equals(surveyId)) {
            throw BusinessException.notFound("题目不存在");
        }

        if (question.getQuestionContent() != null) {
            existing.setQuestionContent(question.getQuestionContent());
        }
        if (question.getQuestionType() != null) {
            existing.setQuestionType(question.getQuestionType());
        }
        if (question.getOptions() != null) {
            existing.setOptions(question.getOptions());
        }
        if (question.getSortOrder() != null) {
            existing.setSortOrder(question.getSortOrder());
        }
        if (question.getIsRequired() != null) {
            existing.setIsRequired(question.getIsRequired());
        }
        questionMapper.updateById(existing);
    }

    /**
     * 删除题目
     */
    public void deleteQuestion(Long surveyId, Long questionId, Long userId) {
        Survey s = surveyMapper.selectById(surveyId);
        if (s == null) {
            throw BusinessException.notFound("问卷不存在");
        }
        if (!s.getCreateBy().equals(userId)) {
            throw BusinessException.forbidden("无权操作");
        }

        // 同时删除该题目的所有回答
        responseMapper.delete(new LambdaQueryWrapper<SurveyResponse>().eq(SurveyResponse::getQuestionId, questionId));
        questionMapper.deleteById(questionId);
    }

    /**
     * 获取问卷题目列表
     */
    public List<SurveyQuestion> getQuestions(Long surveyId) {
        return questionMapper.selectList(new LambdaQueryWrapper<SurveyQuestion>()
                .eq(SurveyQuestion::getSurveyId, surveyId)
                .orderByAsc(SurveyQuestion::getSortOrder));
    }

    // ==================== 问卷提交 ====================
    /**
     * 批量提交问卷答案
     */
    @Transactional
    public void submitResponse(Long surveyId, SurveySubmitDTO dto, Long userId) {
        Survey s = surveyMapper.selectById(surveyId);
        if (s == null) {
            throw BusinessException.notFound("问卷不存在");
        }

        // 检查是否已提交
        Long count = responseMapper.selectCount(new LambdaQueryWrapper<SurveyResponse>()
                .eq(SurveyResponse::getSurveyId, surveyId)
                .eq(SurveyResponse::getRespondentId, userId));
        if (count > 0) {
            throw new BusinessException(400, "您已提交过该问卷，请勿重复提交");
        }

        // 检查是否在有效期内
        LocalDate today = LocalDate.now();
        if (s.getStartDate() != null && today.isBefore(s.getStartDate())) {
            throw new BusinessException(400, "问卷尚未开始");
        }
        if (s.getEndDate() != null && today.isAfter(s.getEndDate())) {
            throw new BusinessException(400, "问卷已结束");
        }

        // 验证必填题目
        List<SurveyQuestion> questions = getQuestions(surveyId);
        Set<Long> answeredIds = dto.getAnswers() != null
                ? dto.getAnswers().stream().map(SurveySubmitDTO.AnswerItem::getQuestionId).collect(Collectors.toSet())
                : Collections.emptySet();

        for (SurveyQuestion q : questions) {
            if (q.getIsRequired() != null && q.getIsRequired() == 1 && !answeredIds.contains(q.getId())) {
                throw new BusinessException(400, "请回答所有必填题目: " + q.getQuestionContent());
            }
        }

        // 批量插入回答
        if (dto.getAnswers() != null) {
            for (SurveySubmitDTO.AnswerItem ai : dto.getAnswers()) {
                SurveyResponse r = new SurveyResponse();
                r.setSurveyId(surveyId);
                r.setQuestionId(ai.getQuestionId());
                r.setRespondentId(userId);
                r.setAnswerContent(ai.getAnswerContent());
                responseMapper.insert(r);
            }
        }
    }

    /**
     * 检查学生是否已提交问卷
     */
    public Boolean checkSubmitted(Long surveyId, Long userId) {
        Long count = responseMapper.selectCount(new LambdaQueryWrapper<SurveyResponse>()
                .eq(SurveyResponse::getSurveyId, surveyId)
                .eq(SurveyResponse::getRespondentId, userId));
        return count > 0;
    }

    /**
     * 获取学生本人的提交答案
     */
    public List<SurveyResponse> getMySubmission(Long surveyId, Long userId) {
        return responseMapper.selectList(new LambdaQueryWrapper<SurveyResponse>()
                .eq(SurveyResponse::getSurveyId, surveyId)
                .eq(SurveyResponse::getRespondentId, userId));
    }

    // ==================== 统计 ====================
    /**
     * 问卷统计（教师视角）：按题目聚合
     */
    public SurveyStatsVO getStats(Long surveyId) {
        Survey s = surveyMapper.selectById(surveyId);
        if (s == null) {
            throw BusinessException.notFound("问卷不存在");
        }

        List<SurveyQuestion> questions = getQuestions(surveyId);
        List<SurveyResponse> responses = responseMapper.selectList(
                new LambdaQueryWrapper<SurveyResponse>().eq(SurveyResponse::getSurveyId, surveyId));

        // 统计参与人数（去重）
        Set<Long> uniqueRespondents = responses.stream()
                .map(SurveyResponse::getRespondentId).collect(Collectors.toSet());

        SurveyStatsVO vo = new SurveyStatsVO();
        vo.setSurveyId(surveyId);
        vo.setSurveyTitle(s.getTitle());
        vo.setTotalRespondents(uniqueRespondents.size());
        vo.setQuestionCount(questions.size());

        List<SurveyStatsVO.QuestionStat> questionStats = new ArrayList<>();
        for (SurveyQuestion q : questions) {
            SurveyStatsVO.QuestionStat qs = new SurveyStatsVO.QuestionStat();
            qs.setQuestionId(q.getId());
            qs.setQuestionContent(q.getQuestionContent());
            qs.setQuestionType(q.getQuestionType());

            List<SurveyResponse> qResponses = responses.stream()
                    .filter(r -> r.getQuestionId().equals(q.getId()))
                    .collect(Collectors.toList());
            qs.setTotalAnswers(qResponses.size());

            if ("SINGLE".equals(q.getQuestionType()) || "MULTI".equals(q.getQuestionType())) {
                // 统计各选项数量
                Map<String, Long> optionCounts = new LinkedHashMap<>();
                try {
                    List<String> options = objectMapper.readValue(q.getOptions(),
                            new TypeReference<List<String>>() {
                    });
                    for (String opt : options) {
                        optionCounts.put(opt, 0L);
                    }
                } catch (Exception ignored) {
                }
                for (SurveyResponse r : qResponses) {
                    if (r.getAnswerContent() != null) {
                        for (String ans : r.getAnswerContent().split(",")) {
                            String trimmed = ans.trim();
                            optionCounts.merge(trimmed, 1L, Long::sum);
                        }
                    }
                }
                qs.setOptionCounts(optionCounts);
            } else {
                // 问答题：收集文本回答
                List<String> textAnswers = qResponses.stream()
                        .map(SurveyResponse::getAnswerContent)
                        .filter(Objects::nonNull)
                        .limit(50)
                        .collect(Collectors.toList());
                qs.setTextAnswers(textAnswers);
            }

            questionStats.add(qs);
        }
        vo.setQuestionStats(questionStats);
        return vo;
    }

    /**
     * 获取问卷原始回答数据（用于列表展示）
     */
    public List<SurveyResponse> getRawResponses(Long surveyId) {
        return responseMapper.selectList(new LambdaQueryWrapper<SurveyResponse>()
                .eq(SurveyResponse::getSurveyId, surveyId));
    }

    // ==================== 管理端统计 ====================
    /**
     * 管理端 - 问卷总览统计
     */
    public AdminSurveyStatsVO getAdminStats() {
        AdminSurveyStatsVO vo = new AdminSurveyStatsVO();

        // 总问卷数
        List<Survey> allSurveys = surveyMapper.selectList(null);
        vo.setTotalSurveys((long) allSurveys.size());

        // 进行中问卷数
        long activeCount = allSurveys.stream()
                .filter(s -> "进行中".equals(s.getStatus()))
                .count();
        vo.setActiveSurveys(activeCount);

        // 总回收数
        List<SurveyResponse> allResponses = responseMapper.selectList(null);
        vo.setTotalResponses((long) allResponses.size());

        // 按课程统计
        Map<Long, List<Survey>> courseSurveyMap = allSurveys.stream()
                .filter(s -> s.getCourseId() != null)
                .collect(Collectors.groupingBy(Survey::getCourseId));

        List<AdminSurveyStatsVO.CourseSurveyStat> courseStats = new ArrayList<>();
        for (Map.Entry<Long, List<Survey>> entry : courseSurveyMap.entrySet()) {
            AdminSurveyStatsVO.CourseSurveyStat cs = new AdminSurveyStatsVO.CourseSurveyStat();
            cs.setCourseId(entry.getKey());
            cs.setSurveyCount((long) entry.getValue().size());

            // 课程名
            Course course = courseMapper.selectById(entry.getKey());
            cs.setCourseName(course != null ? course.getCourseName() : "未知课程");

            // 该课程下所有问卷的回收数
            long respCount = allResponses.stream()
                    .filter(r -> entry.getValue().stream().anyMatch(s -> s.getId().equals(r.getSurveyId())))
                    .count();
            cs.setResponseCount(respCount);
            courseStats.add(cs);
        }
        courseStats.sort((a, b) -> Long.compare(b.getResponseCount(), a.getResponseCount()));
        vo.setCourseStats(courseStats);

        // 最近问卷（取前10个）
        List<Survey> sortedSurveys = allSurveys.stream()
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .limit(10)
                .collect(Collectors.toList());

        List<AdminSurveyStatsVO.RecentSurvey> recents = new ArrayList<>();
        for (Survey s : sortedSurveys) {
            AdminSurveyStatsVO.RecentSurvey rs = new AdminSurveyStatsVO.RecentSurvey();
            rs.setId(s.getId());
            rs.setTitle(s.getTitle());
            rs.setStatus(s.getStatus());

            Course course = courseMapper.selectById(s.getCourseId());
            rs.setCourseName(course != null ? course.getCourseName() : "-");

            // 题目数
            Long qCount = questionMapper.selectCount(
                    new LambdaQueryWrapper<SurveyQuestion>().eq(SurveyQuestion::getSurveyId, s.getId()));
            rs.setQuestionCount(qCount.intValue());

            // 回收数
            Long rCount = responseMapper.selectCount(
                    new LambdaQueryWrapper<SurveyResponse>().eq(SurveyResponse::getSurveyId, s.getId()));
            rs.setResponseCount(rCount.intValue());

            recents.add(rs);
        }
        vo.setRecentSurveys(recents);

        return vo;
    }

    // ==================== 辅助方法 ====================
    private SurveyVO toSurveyVO(Survey s, Long userId) {
        SurveyVO vo = new SurveyVO();
        vo.setId(s.getId());
        vo.setTitle(s.getTitle());
        vo.setDescription(s.getDescription());
        vo.setCourseId(s.getCourseId());
        vo.setStartDate(s.getStartDate());
        vo.setEndDate(s.getEndDate());
        vo.setStatus(s.getStatus());
        vo.setCreateBy(s.getCreateBy());
        vo.setCreateTime(s.getCreateTime());

        // 课程名
        if (s.getCourseId() != null) {
            Course course = courseMapper.selectById(s.getCourseId());
            vo.setCourseName(course != null ? course.getCourseName() : null);
        }

        // 创建者
        if (s.getCreateBy() != null) {
            SysUser user = userMapper.selectById(s.getCreateBy());
            vo.setCreatorName(user != null ? user.getNickname() : null);
        }

        // 题目数
        Long qCount = questionMapper.selectCount(
                new LambdaQueryWrapper<SurveyQuestion>().eq(SurveyQuestion::getSurveyId, s.getId()));
        vo.setQuestionCount(qCount.intValue());

        // 回收数
        Long rCount = responseMapper.selectCount(
                new LambdaQueryWrapper<SurveyResponse>().eq(SurveyResponse::getSurveyId, s.getId()));
        vo.setResponseCount(rCount.intValue());

        // 是否已提交（学生）
        if (userId != null) {
            Long count = responseMapper.selectCount(new LambdaQueryWrapper<SurveyResponse>()
                    .eq(SurveyResponse::getSurveyId, s.getId())
                    .eq(SurveyResponse::getRespondentId, userId));
            vo.setSubmitted(count > 0);
        }

        // 题目列表
        vo.setQuestions(getQuestions(s.getId()));

        return vo;
    }

    /**
     * 获取单个问卷（实体，用于旧接口兼容）
     */
    public Survey getById(Long surveyId) {
        return surveyMapper.selectById(surveyId);
    }
}
