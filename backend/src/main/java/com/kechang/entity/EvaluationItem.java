package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价指标项实体
 */
@Data
@TableName("evaluation_item")
public class EvaluationItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long evaluationId;
    private String itemContent;
    private String itemType;       // RADIO / TEXT
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
