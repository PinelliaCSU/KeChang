package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 随堂测验学生作答
 */
@Data
@TableName("class_quiz_answer")
public class ClassQuizAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long quizId;
    private Long questionId;
    private Long studentId;
    private String answer;
    private Integer isCorrect;
    private LocalDateTime submitTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
