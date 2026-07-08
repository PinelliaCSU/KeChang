package com.kechang.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 问卷提交 DTO（批量提交所有题目答案）
 */
@Data
public class SurveySubmitDTO {

    @NotNull(message = "问卷ID不能为空")
    private Long surveyId;

    /**
     * 答案列表
     */
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {

        private Long questionId;
        private String answerContent;
    }
}
