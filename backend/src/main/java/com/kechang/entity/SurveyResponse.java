package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问卷回答实体
 */
@Data
@TableName("survey_response")
public class SurveyResponse {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long surveyId;
    private Long questionId;
    private Long respondentId;
    private String answerContent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
