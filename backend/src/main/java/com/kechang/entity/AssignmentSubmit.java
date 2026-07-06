package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业提交实体
 */
@Data
@TableName("assignment_submit")
public class AssignmentSubmit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;
    private Long studentId;
    private String content;
    private String attachmentUrl;
    private LocalDateTime submitTime;
    private Double score;
    private String status;        // 待批改 / 已批改

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
