package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 讨论帖/提问实体
 */
@Data
@TableName("question_post")
public class QuestionPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private String title;
    private String content;
    private String postType;      // 提问 / 讨论
    private Long createBy;
    private Integer viewCount;
    private Integer replyCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private String courseName;
}
