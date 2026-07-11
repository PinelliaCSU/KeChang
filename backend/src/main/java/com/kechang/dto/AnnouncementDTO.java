package com.kechang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 公告创建/更新 DTO
 */
@Data
public class AnnouncementDTO {

    @NotBlank(message = "公告标题不能为空")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    /** 课程ID（非空则为课程公告，教师端使用） */
    private Long courseId;

    /** 发送范围：all=全平台, teachers=所有教师, students=所有学生（管理端使用） */
    private String scope;
}
