package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件文件实体 — 支持作业和提交的多文件上传
 */
@Data
@TableName("attachment_file")
public class AttachmentFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String targetType;   // ASSIGNMENT / SUBMIT
    private Long targetId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
