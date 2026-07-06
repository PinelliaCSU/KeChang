package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试记录实体
 */
@Data
@TableName("exam_record")
public class ExamRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;
    private Long studentId;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Double totalScore;
    private Integer passStatus;    // 0否 / 1是
    private Integer isSubmitted;   // 0否 / 1是
    private Integer cheatCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
