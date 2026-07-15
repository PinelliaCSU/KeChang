package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_knowledge_qa_history")
public class CourseKnowledgeQaHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private Long userId;
    private String question;
    private String answer;
    private String referencesJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
