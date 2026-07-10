package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实训资料实体
 */
@Data
@TableName("training_material")
public class TrainingMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String title;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
