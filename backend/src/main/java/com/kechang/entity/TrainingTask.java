package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实训任务实体
 */
@Data
@TableName("training_task")
public class TrainingTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String taskName;
    private String description;
    private String attachmentUrl;
    private LocalDateTime deadline;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
