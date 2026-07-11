package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 试题实体
 */
@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String questionType;   // SINGLE / MULTI / JUDGE / FILL / ESSAY
    private String content;
    private String options;        // JSON 字符串
    private String answer;
    private String analysis;
    private Double score;
    private Integer difficulty;    // 1-5
    private String knowledgePoint;
    private Long courseId;
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    /**
     * 编程题配置（JSON）：仅 questionType = CODE 时使用
     */
    private String programmingConfig;
}
