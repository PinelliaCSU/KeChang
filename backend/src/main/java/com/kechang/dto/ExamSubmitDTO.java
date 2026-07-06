package com.kechang.dto;

import lombok.Data;

@Data
public class ExamSubmitDTO {

    private List<AnswerItem> answers;
    private Integer cheatCount;

    @Data
    public static class AnswerItem {

        private Long questionId;
        private String answer;
    }
}
