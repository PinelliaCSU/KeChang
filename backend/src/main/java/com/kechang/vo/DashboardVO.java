package com.kechang.vo;

import lombok.Data;

import java.util.List;

/**
 * 仪表盘统计 VO
 */
@Data
public class DashboardVO {

    private Long courseCount;
    private Long pendingAssignmentCount;
    private Long pendingQuestionCount;
    private Double avgScore;

    // 近7日活跃趋势
    private List<DailyActive> activeTrend;

    @Data
    public static class DailyActive {

        private String date;
        private Integer count;
    }
}
