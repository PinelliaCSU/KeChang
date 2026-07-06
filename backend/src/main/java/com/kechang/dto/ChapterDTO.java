package com.kechang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 章节创建/编辑 DTO
 */
@Data
public class ChapterDTO {

    @NotBlank(message = "章节名称不能为空")
    private String chapterName;

    @NotNull(message = "排序不能为空")
    private Integer sortOrder;

    private String description;
}
