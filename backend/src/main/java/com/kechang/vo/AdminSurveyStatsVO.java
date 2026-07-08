package com.kechang.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 管理端问卷统计 VO
 */
@Data
public class AdminSurveyStatsVO {

    /**
     * 总问卷数
     */
    private Long totalSurveys;

    /**
     * 进行中问卷数
     */
    private Long activeSurveys;

    /**
     * 总回收数
     */
    private Long totalResponses;

    /**
     * 平均回收率
     */
    private Double avgResponseRate;

    /**
     * 课程维度统计
     */
    private List<CourseSurveyStat> courseStats;

    /**
     * 最近问卷列表
     */
    private List<RecentSurvey> recentSurveys;

    @Data
    public static class CourseSurveyStat {

        private Long courseId;
        private String courseName;
        private Long surveyCount;
        private Long responseCount;
    }

    @Data
    public static class RecentSurvey {

        private Long id;
        private String title;
        private String courseName;
        private String status;
        private Integer responseCount;
        private Integer questionCount;
    }
}
