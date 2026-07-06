package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 答题详情实体
 */
@Data
@TableName("exam_record_answer")
public class ExamRecordAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;
    private Long questionId;
    private String studentAnswer;
    private Integer isCorrect;     // 0错误 / 1正确
    private Double score;
    private Long markedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
