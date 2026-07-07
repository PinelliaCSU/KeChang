package com.kechang.vo;

import lombok.Data;

import java.time.LocalDateTime;

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
    private Integer submitCount;
    private Integer totalStudents;
}
