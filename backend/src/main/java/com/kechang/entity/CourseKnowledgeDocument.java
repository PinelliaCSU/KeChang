package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_knowledge_document")
public class CourseKnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private String title;
    private String sourceType;
    private Long sourceId;
    private String sourceUrl;
    private String status;
    private Integer chunkCount;
    private Integer charCount;
    private String errorMessage;
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
