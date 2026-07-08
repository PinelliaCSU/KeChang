package com.kechang.vo;

import com.kechang.entity.EvaluationItem;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价详情 VO（含课程名、教师名、指标项、提交状态）
 */
@Data
public class EvaluationVO {

    private Long id;
    private Long courseId;
    private String courseName;
    private String teacherName;
    private Long teacherId;
    private String evaluationName;
    private String evaluationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createTime;

    /**
     * 评价指标项列表
     */
    private List<EvaluationItem> items;

    /**
     * 当前学生是否已提交
     */
    private Boolean submitted;
}
