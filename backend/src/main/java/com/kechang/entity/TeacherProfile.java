package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师扩展信息实体
 */
@Data
@TableName("teacher_profile")
public class TeacherProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String realName;
    private String title;
    private String department;
    private String introduction;
    private String avatar;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
