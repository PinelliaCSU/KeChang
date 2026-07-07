package com.kechang.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试列表 VO（含课程名称）
 */
@Data
public class ExamListVO {

    private Long id;
    private String examName;
    private Long paperId;
    private Long courseId;
    private String courseName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private String status;
    private Integer allowRetry;
    private LocalDateTime createTime;
}
