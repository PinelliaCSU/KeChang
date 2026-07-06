package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价结果实体
 */
@Data
@TableName("evaluation_result")
public class EvaluationResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long evaluationId;
    private Long evaluatorId;
    private Long targetUserId;
    private Long itemId;
    private Integer score;
    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
