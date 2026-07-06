package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回答实体
 */
@Data
@TableName("answer")
public class Answer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    private String content;
    private Long createBy;
    private Integer isAccepted;   // 0否 / 1是

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
