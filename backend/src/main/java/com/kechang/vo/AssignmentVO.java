package com.kechang.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业 VO（含课程名称、提交统计）
 */
@Data
public class AssignmentVO {

    private Long id;
    private Long courseId;
    private String courseName;
    private String title;
    private String content;
    private String attachmentUrl;
    private LocalDateTime deadline;
    private Double totalScore;
    private String status;
    private Long createBy;
    private LocalDateTime createTime;

    // 统计字段
    private Integer submitCount;
    private Integer totalStudents;
    // 当前学生的提交状态（学生端用）
    private String mySubmitStatus;
    private Double myScore;
}
