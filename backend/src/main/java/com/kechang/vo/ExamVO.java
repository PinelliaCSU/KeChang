package com.kechang.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试详情 VO（教师视角含答案）
 */
@Data
public class ExamVO {

    private Long id;
    private String examName;
    private Long paperId;
    private String paperName;
    private Long courseId;
    private String courseName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private Double totalScore;
    private Double passScore;
    private String status;
    private Integer allowRetry;

    // 学生视角：不返回题目答案
    // 教师视角：返回完整题目含答案
    private List<QuestionVO> questions;
}
