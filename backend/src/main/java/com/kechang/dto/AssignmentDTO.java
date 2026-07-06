package com.kechang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业创建 DTO
 */
@Data
public class AssignmentDTO {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotBlank(message = "作业标题不能为空")
    private String title;

    private String content;
    private String attachmentUrl;

    @NotNull(message = "截止时间不能为空")
    private LocalDateTime deadline;

    private Double totalScore;
}
