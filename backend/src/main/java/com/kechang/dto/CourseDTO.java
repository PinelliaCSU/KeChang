package com.kechang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 课程创建/编辑 DTO
 */
@Data
public class CourseDTO {

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    private String coverImg;
    private String description;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    private String status;
}
