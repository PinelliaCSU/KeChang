package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 问卷实体
 */
@Data
@TableName("survey")
public class Survey {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String description;
    private Long courseId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
