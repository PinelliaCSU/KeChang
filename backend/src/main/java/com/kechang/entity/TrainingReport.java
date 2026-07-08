package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实训报告实体
 */
@Data
@TableName("training_report")
public class TrainingReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long studentId;
    private String reportContent;
    private String attachmentUrl;
    private LocalDateTime submitTime;
    private Double score;
    private String comment;
    private String status;

    @TableField(exist = false)
    private String studentName;

    @TableField(exist = false)
    private String studentNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
