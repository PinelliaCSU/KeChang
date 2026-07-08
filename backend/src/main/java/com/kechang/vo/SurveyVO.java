package com.kechang.vo;

import com.kechang.entity.SurveyQuestion;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 问卷详情 VO
 */
@Data
public class SurveyVO {

    private Long id;
    private String title;
    private String description;
    private Long courseId;
    private String courseName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long createBy;
    private String creatorName;
    private LocalDateTime createTime;

    /**
     * 题目列表
     */
    private List<SurveyQuestion> questions;

    /**
     * 统计
     */
    private Integer questionCount;
    private Integer responseCount;

    /**
     * 当前学生是否已提交
     */
    private Boolean submitted;
}
