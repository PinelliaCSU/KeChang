package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 实训项目实体
 */
@Data
@TableName("training_project")
public class TrainingProject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String projectName;
    private Long courseId;
    private String description;
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
