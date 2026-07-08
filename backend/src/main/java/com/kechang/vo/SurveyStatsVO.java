package com.kechang.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 问卷统计 VO
 */
@Data
public class SurveyStatsVO {

    /**
     * 问卷基本信息
     */
    private Long surveyId;
    private String surveyTitle;
    private Integer totalRespondents;    // 参与人数
    private Integer questionCount;       // 题目数

    /**
     * 每道题的统计
     */
    private List<QuestionStat> questionStats;

    @Data
    public static class QuestionStat {

        private Long questionId;
        private String questionContent;
        private String questionType;
        private Integer totalAnswers;       // 本题回答人次

        // 选择题统计：选项 -> 数量
        private Map<String, Long> optionCounts;

        // 问答题统计：所有文本回答列表（最多展示50条）
        private List<String> textAnswers;
    }
}
