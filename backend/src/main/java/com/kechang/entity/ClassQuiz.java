package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 随堂测验会话
 */
@Data
@TableName("class_quiz")
public class ClassQuiz {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private Long questionId;
    private String title;
    private Integer durationSeconds;
    private String status; // ACTIVE / ENDED
    private Long createBy;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
