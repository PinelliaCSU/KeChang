package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程资源实体
 */
@Data
@TableName("course_resource")
public class CourseResource {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private Long chapterId;
    private String resourceName;
    private String resourceType;  // VIDEO / PDF / PPT / LINK
    private String fileUrl;
    private Integer sortOrder;
    private Long fileSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
