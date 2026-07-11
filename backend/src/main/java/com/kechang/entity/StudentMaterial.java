package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生上传资料实体
 */
@Data
@TableName("student_material")
public class StudentMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long courseId;
    private String courseName;
    private String title;
    private String description;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
