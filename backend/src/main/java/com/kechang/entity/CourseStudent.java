package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 选课关联实体
 */
@Data
@TableName("course_student")
public class CourseStudent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private Long studentId;
    private LocalDateTime enrolledTime;
    private Double progress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
