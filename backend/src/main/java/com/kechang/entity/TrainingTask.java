package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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

    @TableField(exist = false)
    private Long reportCount;

    @TableField(exist = false)
    private Long myReportId;

    @TableField(exist = false)
    private String reportContent;

    @TableField(exist = false)
    private String reportAttachmentUrl;

    @TableField(exist = false)
    private LocalDateTime submitTime;

    @TableField(exist = false)
    private Double score;

    @TableField(exist = false)
    private String comment;

    @TableField(exist = false)
    private String reportStatus;

    // 多文件附件（输入）
    @TableField(exist = false)
    private List<String> attachmentUrls;
    @TableField(exist = false)
    private List<String> attachmentNames;
    @TableField(exist = false)
    private List<Long> attachmentSizes;
    // 多文件附件（输出）
    @TableField(exist = false)
    private List<AttachmentFile> attachments;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
