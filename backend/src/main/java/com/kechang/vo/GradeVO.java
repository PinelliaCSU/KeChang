package com.kechang.vo;

import lombok.Data;

import java.util.List;

/**
 * 学生成绩 VO
 */
@Data
public class GradeVO {

    private Long courseId;
    private String courseName;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private Double examAvgScore;
    private Double assignmentAvgScore;
    private Double totalScore;
    private String gradeLevel;

    // 成绩分布
    private List<Distribution> distribution;

    @Data
    public static class Distribution {

        private String level;
        private Integer count;
    }
}
