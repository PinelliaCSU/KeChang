package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程总评成绩实体
 */
@Data
@TableName("grade_course")
public class GradeCourse {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private Long studentId;
    private Double examAvgScore;
    private Double assignmentAvgScore;
    private Double totalScore;
    private String gradeLevel;    // 优秀 / 良好 / 中等 / 及格 / 不及格

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
