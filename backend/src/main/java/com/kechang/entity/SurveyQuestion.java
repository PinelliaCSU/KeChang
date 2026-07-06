package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问卷题目实体
 */
@Data
@TableName("survey_question")
public class SurveyQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long surveyId;
    private String questionContent;
    private String questionType;   // SINGLE / MULTI / TEXT
    private String options;        // JSON 格式
    private Integer sortOrder;
    private Integer isRequired;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
