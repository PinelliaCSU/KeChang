package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.PageResult;
import com.kechang.entity.Survey;
import com.kechang.entity.SurveyQuestion;
import com.kechang.entity.SurveyResponse;
import com.kechang.mapper.SurveyMapper;
import com.kechang.mapper.SurveyQuestionMapper;
import com.kechang.mapper.SurveyResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 问卷服务
 */
@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyMapper surveyMapper;
    private final SurveyQuestionMapper questionMapper;
    private final SurveyResponseMapper responseMapper;

    public PageResult<Survey> list(int page, int size, Long courseId) {
        LambdaQueryWrapper<Survey> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(Survey::getCourseId, courseId);
        }
        Page<Survey> p = surveyMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public void create(Survey s) {
        surveyMapper.insert(s);
    }

    public void submitResponse(SurveyResponse r) {
        responseMapper.insert(r);
    }

    public List<SurveyQuestion> getQuestions(Long surveyId) {
        return questionMapper.selectList(new LambdaQueryWrapper<SurveyQuestion>()
                .eq(SurveyQuestion::getSurveyId, surveyId).orderByAsc(SurveyQuestion::getSortOrder));
    }

    public List<SurveyResponse> getStats(Long surveyId) {
        return responseMapper.selectList(new LambdaQueryWrapper<SurveyResponse>()
                .eq(SurveyResponse::getSurveyId, surveyId));
    }
}
