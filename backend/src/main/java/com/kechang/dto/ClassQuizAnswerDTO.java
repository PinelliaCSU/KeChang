package com.kechang.dto;

import lombok.Data;

import java.util.List;

@Data
public class ClassQuizAnswerDTO {

    private Object answer;
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private Object answer;
    }
}
