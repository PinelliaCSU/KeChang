package com.kechang.controller;

import com.kechang.common.Result;
import com.kechang.entity.Schedule;
import com.kechang.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 日程控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    private Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ==================== 查询 ====================
    /**
     * 查询指定月份的日程（日历月视图用）
     */
    @GetMapping("/month")
    public Result<List<Schedule>> listByMonth(@RequestParam int year, @RequestParam int month) {
        return Result.success(scheduleService.listByMonth(getUserId(), year, month));
    }

    /**
     * 查询指定日期的日程列表
     */
    @GetMapping("/date")
    public Result<List<Schedule>> listByDate(@RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        return Result.success(scheduleService.listByDate(getUserId(), localDate));
    }

    /**
     * 查询日程详情
     */
    @GetMapping("/{id}")
    public Result<Schedule> getById(@PathVariable Long id) {
        return Result.success(scheduleService.getById(id, getUserId()));
    }

    // ==================== 新增 ====================
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER','ROLE_ADMIN')")
    public Result<Schedule> create(@RequestBody Schedule schedule) {
        return Result.success(scheduleService.create(schedule, getUserId()));
    }

    // ==================== 更新 ====================
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER','ROLE_ADMIN')")
    public Result<Schedule> update(@PathVariable Long id, @RequestBody Schedule schedule) {
        return Result.success(scheduleService.update(id, schedule, getUserId()));
    }

    // ==================== 删除 ====================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER','ROLE_ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id, getUserId());
        return Result.success();
    }
}
