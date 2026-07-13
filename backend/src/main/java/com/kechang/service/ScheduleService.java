package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kechang.common.BusinessException;
import com.kechang.entity.Schedule;
import com.kechang.mapper.ScheduleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 日程服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleMapper scheduleMapper;

    // ==================== 查询 ====================
    /**
     * 查询指定月份的事件（用于日历月视图）
     */
    public List<Schedule> listByMonth(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getUserId, userId)
                .ge(Schedule::getScheduleDate, start)
                .le(Schedule::getScheduleDate, end)
                .orderByAsc(Schedule::getScheduleDate)
                .orderByAsc(Schedule::getStartTime);
        return scheduleMapper.selectList(wrapper);
    }

    /**
     * 查询指定日期的日程列表
     */
    public List<Schedule> listByDate(Long userId, LocalDate date) {
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getUserId, userId)
                .eq(Schedule::getScheduleDate, date)
                .orderByAsc(Schedule::getStartTime);
        return scheduleMapper.selectList(wrapper);
    }

    /**
     * 查询日程详情
     */
    public Schedule getById(Long id, Long userId) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw BusinessException.notFound("日程不存在");
        }
        if (!schedule.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权查看该日程");
        }
        return schedule;
    }

    // ==================== 新增 ====================
    @Transactional
    public Schedule create(Schedule schedule, Long userId) {
        if (!StringUtils.hasText(schedule.getTitle())) {
            throw BusinessException.badRequest("日程标题不能为空");
        }
        if (schedule.getScheduleDate() == null) {
            throw BusinessException.badRequest("日程日期不能为空");
        }
        schedule.setUserId(userId);
        if (!StringUtils.hasText(schedule.getType())) {
            schedule.setType("PERSONAL");
        }
        if (!StringUtils.hasText(schedule.getColor())) {
            schedule.setColor(getDefaultColor(schedule.getType()));
        }
        scheduleMapper.insert(schedule);
        return schedule;
    }

    // ==================== 更新 ====================
    @Transactional
    public Schedule update(Long id, Schedule schedule, Long userId) {
        Schedule existing = scheduleMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("日程不存在");
        }
        if (!existing.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权修改该日程");
        }
        schedule.setId(id);
        schedule.setUserId(userId); // 不允许修改所有人
        scheduleMapper.updateById(schedule);
        return scheduleMapper.selectById(id);
    }

    // ==================== 删除 ====================
    @Transactional
    public void delete(Long id, Long userId) {
        Schedule existing = scheduleMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("日程不存在");
        }
        if (!existing.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权删除该日程");
        }
        scheduleMapper.deleteById(id);
    }

    // ==================== 辅助方法 ====================
    private String getDefaultColor(String type) {
        return switch (type) {
            case "COURSE" ->
                "#67C23A";
            case "EXAM" ->
                "#E6A23C";
            case "TRAINING" ->
                "#409EFF";
            default ->
                "#409EFF"; // PERSONAL
        };
    }
}
