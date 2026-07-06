package com.kechang.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 课程详情 VO
 */
@Data
public class CourseVO {

    private Long id;
    private String courseName;
    private String coverImg;
    private String description;
    private Long teacherId;
    private String teacherName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Integer studentCount;

    // 章节列表
    private List<ChapterVO> chapters;
    // 学生学习进度（教师视角）
    private Double progress;
}
