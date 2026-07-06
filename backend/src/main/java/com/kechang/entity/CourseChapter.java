package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程章节实体
 */
@Data
@TableName("course_chapter")
public class CourseChapter {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private String chapterName;
    private Integer sortOrder;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
