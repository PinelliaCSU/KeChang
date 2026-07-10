package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("participation_point")
public class ParticipationPoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private Long studentId;
    private Long teacherId;
    private Integer points;
    private String sourceType;
    private String reason;
    private LocalDateTime eventTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
