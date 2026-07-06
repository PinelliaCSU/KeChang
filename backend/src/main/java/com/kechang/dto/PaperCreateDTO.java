package com.kechang.dto;

import lombok.Data;

import java.util.List;

/**
 * 手动组卷 DTO
 */
@Data
public class PaperCreateDTO {

    @NotBlank(message = "试卷名称不能为空")
    private String paperName;

    private Long courseId;
    private Double totalScore;
    private Integer duration;
    private Double passScore;

    private List<PaperQuestionItem> questions;

    @Data
    public static class PaperQuestionItem {

        private Long questionId;
        private Double questionScore;
        private Integer sortOrder;
    }
}
