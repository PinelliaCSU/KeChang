package com.kechang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 问卷创建/编辑 DTO（含题目列表）
 */
@Data
public class SurveyCreateDTO {

    @NotBlank(message = "问卷标题不能为空")
    private String title;

    private String description;
    private Long courseId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    /**
     * 问卷题目列表
     */
    private List<QuestionItem> questions;

    @Data
    public static class QuestionItem {

        private String questionContent;
        private String questionType;   // SINGLE / MULTI / TEXT
        private String options;        // JSON 字符串
        private Integer sortOrder;
        private Integer isRequired;
    }
}
