package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业评分实体
 */
@Data
@TableName("assignment_grade")
public class AssignmentGrade {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submitId;
    private Double score;
    private String comment;
    private String feedbackMarks;
    private Long gradedBy;
    private LocalDateTime gradedTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
