package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生错题集实体
 */
@Data
@TableName("student_wrong_question")
public class StudentWrongQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long questionId;
    private Long courseId;
    private String courseName;
    private String sourceType;
    private Long sourceId;
    private String questionType;
    private String questionContent;
    private String options;
    private String wrongAnswer;
    private String correctAnswer;
    private String analysis;
    private String knowledgePoint;
    private Integer isMastered;
    private Integer wrongCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
