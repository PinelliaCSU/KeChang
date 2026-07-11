package com.kechang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseKnowledgeAskDTO {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotBlank(message = "问题不能为空")
    private String question;

    private Integer topK;
}
