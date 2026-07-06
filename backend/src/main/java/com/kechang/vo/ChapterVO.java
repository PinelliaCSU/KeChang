package com.kechang.vo;

import lombok.Data;

/**
 * 章节 VO
 */
@Data
public class ChapterVO {

    private Long id;
    private String chapterName;
    private Integer sortOrder;
    private String description;
    private Integer resourceCount;
}
