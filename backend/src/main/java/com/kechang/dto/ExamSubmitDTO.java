package com.kechang.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExamSubmitDTO {

    private List<AnswerItem> answers;
    private Integer cheatCount;

    @Data
    public static class AnswerItem {

        private Long questionId;
        private Object answer;
    }
}
