package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试安排实体
 */
@Data
@TableName("exam")
public class Exam {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String examName;
    private Long paperId;
    private Long courseId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private String status;         // 未开始 / 进行中 / 已结束
    private Integer allowRetry;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
