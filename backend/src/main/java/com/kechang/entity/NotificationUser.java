package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户通知状态实体
 */
@Data
@TableName("notification_user")
public class NotificationUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long notificationId;
    private Long userId;
    private Integer isRead;        // 0未读 / 1已读
    private LocalDateTime readTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
