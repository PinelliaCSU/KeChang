package com.kechang.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 管理端仪表盘统计 VO
 */
@Data
public class AdminDashboardVO {

    private Long totalUsers;
    private Long teacherCount;
    private Long studentCount;
    private Long courseCount;
    private Long newUsersThisMonth;
    private Long todayActiveOps;

    // 近7日活跃趋势
    private List<DashboardVO.DailyActive> activeTrend;

    // 最近操作记录（简要）
    private List<Map<String, Object>> recentOperations;
}
