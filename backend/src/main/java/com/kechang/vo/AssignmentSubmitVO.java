package com.kechang.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业提交 VO（含学生姓名、学号）
 */
@Data
public class AssignmentSubmitVO {

    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String content;
    private String attachmentUrl;
    private LocalDateTime submitTime;
    private Double score;
    private String status;
    private LocalDateTime createTime;
}
