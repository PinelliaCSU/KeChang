package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 仪表盘服务
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CourseMapper courseMapper;
    private final AssignmentSubmitMapper submitMapper;
    private final QuestionPostMapper postMapper;
    private final ExamRecordMapper recordMapper;
    private final GradeCourseMapper gradeCourseMapper;
    private final CourseStudentMapper courseStudentMapper;

    public DashboardVO getTeacherDashboard(Long teacherId) {
        DashboardVO vo = new DashboardVO();
        // 课程数
        vo.setCourseCount(courseMapper.selectCount(
                new LambdaQueryWrapper<Course>().eq(Course::getTeacherId, teacherId)));
        // 待批改作业
        vo.setPendingAssignmentCount(submitMapper.selectCount(
                new LambdaQueryWrapper<AssignmentSubmit>().eq(AssignmentSubmit::getStatus, "待批改")));
        // 待回复问题
        vo.setPendingQuestionCount(postMapper.selectCount(
                new LambdaQueryWrapper<QuestionPost>().eq(QuestionPost::getReplyCount, 0)));
        // 近7日活跃趋势（模拟数据）
        List<DashboardVO.DailyActive> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            DashboardVO.DailyActive da = new DashboardVO.DailyActive();
            da.setDate(LocalDate.now().minusDays(i).toString());
            da.setCount((int) (Math.random() * 50 + 20));
            trend.add(da);
        }
        vo.setActiveTrend(trend);
        return vo;
    }

    public DashboardVO getStudentDashboard(Long studentId) {
        DashboardVO vo = new DashboardVO();
        vo.setCourseCount(courseStudentMapper.selectCount(
                new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getStudentId, studentId)));
        // 待做作业（简化）
        vo.setPendingAssignmentCount(0L);
        // 学习趋势
        List<DashboardVO.DailyActive> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            DashboardVO.DailyActive da = new DashboardVO.DailyActive();
            da.setDate(LocalDate.now().minusDays(i).toString());
            da.setCount((int) (Math.random() * 8 + 2));
            trend.add(da);
        }
        vo.setActiveTrend(trend);
        return vo;
    }
}
