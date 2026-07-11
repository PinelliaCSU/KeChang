package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_knowledge_chunk")
public class CourseKnowledgeChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;
    private Long courseId;
    private Integer chunkIndex;
    private String content;
    private String sourceTitle;
    private String sourceType;
    private Long sourceId;
    private String sourceUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
