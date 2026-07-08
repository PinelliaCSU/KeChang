package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import com.kechang.vo.EvaluationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评价服务
 */
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationMapper evalMapper;
    private final EvaluationItemMapper itemMapper;
    private final EvaluationResultMapper resultMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper userMapper;
    private final CourseStudentMapper courseStudentMapper;

    /**
     * 分页查询评价列表（教师返回全部，学生只返回已选课程的评价）
     */
    public PageResult<EvaluationVO> list(int page, int size, Long courseId, Long userId, String role) {
        LambdaQueryWrapper<Evaluation> w = new LambdaQueryWrapper<>();
        if (courseId != null) {
            w.eq(Evaluation::getCourseId, courseId);
        }

        // 学生只看到自己已选课程中"进行中"且类型为"学生评教"的评价
        if ("ROLE_STUDENT".equals(role)) {
            List<CourseStudent> enrollments = courseStudentMapper.selectList(
                    new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getStudentId, userId));
            List<Long> enrolledCourseIds = enrollments.stream().map(CourseStudent::getCourseId).collect(Collectors.toList());
            if (enrolledCourseIds.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0, page, size);
            }
            w.in(Evaluation::getCourseId, enrolledCourseIds);
            w.eq(Evaluation::getEvaluationType, "学生评教");
            w.eq(Evaluation::getStatus, "进行中");
        }

        w.orderByDesc(Evaluation::getCreateTime);
        Page<Evaluation> p = evalMapper.selectPage(new Page<>(page, size), w);

        List<EvaluationVO> vos = p.getRecords().stream()
                .map(e -> toEvaluationVO(e, userId))
                .collect(Collectors.toList());
        return PageResult.of(vos, p.getTotal(), page, size);
    }

    /**
     * 创建评价
     */
    @Transactional
    public void create(Evaluation e) {
        if (e.getStatus() == null) {
            e.setStatus("未开始");
        }
        evalMapper.insert(e);
    }

    /**
     * 创建评价（含指标项），接收 Map 格式的请求体
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public void create(Map<String, Object> body) {
        // 1. 创建评价
        Evaluation e = new Evaluation();
        e.setEvaluationName((String) body.get("evaluationName"));
        e.setEvaluationType((String) body.get("evaluationType"));
        if (body.get("courseId") != null) {
            e.setCourseId(Long.valueOf(body.get("courseId").toString()));
        }
        if (body.get("startDate") != null) {
            e.setStartDate(parseDate(body.get("startDate").toString()));
        }
        if (body.get("endDate") != null) {
            e.setEndDate(parseDate(body.get("endDate").toString()));
        }
        e.setStatus((String) body.getOrDefault("status", "未开始"));
        evalMapper.insert(e);

        // 2. 创建指标项
        Object itemsObj = body.get("items");
        if (itemsObj instanceof List) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> itemMap = items.get(i);
                EvaluationItem item = new EvaluationItem();
                item.setEvaluationId(e.getId());
                item.setItemContent((String) itemMap.get("itemContent"));
                item.setItemType((String) itemMap.getOrDefault("itemType", "RADIO"));
                item.setSortOrder(i);
                itemMapper.insert(item);
            }
        }
    }

    /**
     * 更新评价基本信息及指标项
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public void update(Long evalId, Map<String, Object> body) {
        Evaluation e = evalMapper.selectById(evalId);
        if (e == null) {
            throw BusinessException.notFound("评价不存在");
        }

        if (body.get("evaluationName") != null) {
            e.setEvaluationName((String) body.get("evaluationName"));
        }
        if (body.get("evaluationType") != null) {
            e.setEvaluationType((String) body.get("evaluationType"));
        }
        if (body.containsKey("courseId")) {
            e.setCourseId(body.get("courseId") != null ? Long.valueOf(body.get("courseId").toString()) : null);
        }
        if (body.get("startDate") != null) {
            e.setStartDate(parseDate(body.get("startDate").toString()));
        }
        if (body.get("endDate") != null) {
            e.setEndDate(parseDate(body.get("endDate").toString()));
        }
        if (body.get("status") != null) {
            e.setStatus((String) body.get("status"));
        }
        evalMapper.updateById(e);

        // 全量替换指标项
        Object itemsObj = body.get("items");
        if (itemsObj instanceof List) {
            // 删除旧指标项
            itemMapper.delete(new LambdaQueryWrapper<EvaluationItem>()
                    .eq(EvaluationItem::getEvaluationId, evalId));
            // 批量插入新指标项
            List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> itemMap = items.get(i);
                EvaluationItem item = new EvaluationItem();
                item.setEvaluationId(evalId);
                item.setItemContent((String) itemMap.get("itemContent"));
                item.setItemType((String) itemMap.getOrDefault("itemType", "RADIO"));
                item.setSortOrder(i);
                itemMapper.insert(item);
            }
        }
    }

    /**
     * 删除评价（同时删除关联的指标项和评价结果）
     */
    @Transactional
    public void delete(Long evalId) {
        Evaluation e = evalMapper.selectById(evalId);
        if (e == null) {
            throw BusinessException.notFound("评价不存在");
        }
        // 逻辑删除评价
        evalMapper.deleteById(evalId);
        // 物理删除关联的指标项
        itemMapper.delete(new LambdaQueryWrapper<EvaluationItem>()
                .eq(EvaluationItem::getEvaluationId, evalId));
        // 物理删除关联的评价结果
        resultMapper.delete(new LambdaQueryWrapper<EvaluationResult>()
                .eq(EvaluationResult::getEvaluationId, evalId));
    }

    /**
     * 获取评价详情（含指标项）
     */
    public EvaluationVO getDetail(Long evalId, Long userId) {
        Evaluation e = evalMapper.selectById(evalId);
        if (e == null) {
            throw BusinessException.notFound("评价不存在");
        }
        return toEvaluationVO(e, userId);
    }

    /**
     * 获取评价指标项列表
     */
    public List<EvaluationItem> getItems(Long evalId) {
        return itemMapper.selectList(new LambdaQueryWrapper<EvaluationItem>()
                .eq(EvaluationItem::getEvaluationId, evalId)
                .orderByAsc(EvaluationItem::getSortOrder));
    }

    /**
     * 检查用户是否已提交评价
     */
    public Boolean checkSubmitted(Long evalId, Long userId) {
        Long count = resultMapper.selectCount(new LambdaQueryWrapper<EvaluationResult>()
                .eq(EvaluationResult::getEvaluationId, evalId)
                .eq(EvaluationResult::getEvaluatorId, userId));
        return count > 0;
    }

    /**
     * 批量提交评价结果（学生评教）
     */
    @Transactional
    public void submitResults(Long evalId, Long userId, List<EvaluationResult> results) {
        Evaluation e = evalMapper.selectById(evalId);
        if (e == null) {
            throw BusinessException.notFound("评价不存在");
        }

        // 检查是否已提交
        if (checkSubmitted(evalId, userId)) {
            throw new BusinessException(400, "您已提交过该评价，请勿重复提交");
        }

        // 检查是否在有效期内
        LocalDate today = LocalDate.now();
        if (e.getStartDate() != null && today.isBefore(e.getStartDate())) {
            throw new BusinessException(400, "评价尚未开始");
        }
        if (e.getEndDate() != null && today.isAfter(e.getEndDate())) {
            throw new BusinessException(400, "评价已结束");
        }

        // 确定被评价人（学生评教 -> 被评价人是课程的授课教师）
        Long targetUserId = null;
        if ("学生评教".equals(e.getEvaluationType())) {
            if (e.getCourseId() != null) {
                Course course = courseMapper.selectById(e.getCourseId());
                if (course != null) {
                    targetUserId = course.getTeacherId();
                }
            }
        }
        if (targetUserId == null) {
            throw BusinessException.badRequest("无法确定被评价人");
        }

        for (EvaluationResult r : results) {
            r.setEvaluationId(evalId);
            r.setEvaluatorId(userId);
            r.setTargetUserId(targetUserId);
            resultMapper.insert(r);
        }
    }

    /**
     * 获取评价结果列表
     */
    public List<EvaluationResult> getResults(Long evalId) {
        return resultMapper.selectList(new LambdaQueryWrapper<EvaluationResult>()
                .eq(EvaluationResult::getEvaluationId, evalId));
    }

    /**
     * 获取评价详情结果（按评价人分组，显示每个学生的具体评分）
     */
    public List<Map<String, Object>> getResultsWithItems(Long evalId) {
        List<EvaluationResult> results = getResults(evalId);
        List<EvaluationItem> items = getItems(evalId);

        // 按评价人分组
        Map<Long, List<EvaluationResult>> byEvaluator = results.stream()
                .collect(Collectors.groupingBy(EvaluationResult::getEvaluatorId, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<EvaluationResult>> entry : byEvaluator.entrySet()) {
            Map<String, Object> studentResult = new LinkedHashMap<>();
            Long evaluatorId = entry.getKey();
            studentResult.put("evaluatorId", evaluatorId);

            // 查询学生姓名
            SysUser user = userMapper.selectById(evaluatorId);
            studentResult.put("evaluatorName", user != null ? user.getNickname() : "用户" + evaluatorId);

            // 为该学生的每条结果按指标顺序组装
            List<Map<String, Object>> itemResults = new ArrayList<>();
            Map<Long, EvaluationResult> resultByItem = entry.getValue().stream()
                    .collect(Collectors.toMap(EvaluationResult::getItemId, r -> r, (a, b) -> a));
            for (EvaluationItem item : items) {
                Map<String, Object> ir = new LinkedHashMap<>();
                ir.put("itemId", item.getId());
                ir.put("itemContent", item.getItemContent());
                ir.put("itemType", item.getItemType());
                EvaluationResult r = resultByItem.get(item.getId());
                if (r != null) {
                    ir.put("score", r.getScore());
                    ir.put("content", r.getContent());
                } else {
                    ir.put("score", null);
                    ir.put("content", null);
                }
                itemResults.add(ir);
            }
            studentResult.put("items", itemResults);
            result.add(studentResult);
        }
        return result;
    }

    // ==================== 辅助方法 ====================
    private EvaluationVO toEvaluationVO(Evaluation e, Long userId) {
        EvaluationVO vo = new EvaluationVO();
        vo.setId(e.getId());
        vo.setCourseId(e.getCourseId());
        vo.setEvaluationName(e.getEvaluationName());
        vo.setEvaluationType(e.getEvaluationType());
        vo.setStartDate(e.getStartDate());
        vo.setEndDate(e.getEndDate());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());

        // 课程名 + 教师名
        if (e.getCourseId() != null) {
            Course course = courseMapper.selectById(e.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getCourseName());
                vo.setTeacherId(course.getTeacherId());
                if (course.getTeacherId() != null) {
                    SysUser teacher = userMapper.selectById(course.getTeacherId());
                    vo.setTeacherName(teacher != null ? teacher.getNickname() : null);
                }
            }
        }

        // 指标项
        List<EvaluationItem> items = getItems(e.getId());
        vo.setItems(items);

        // 是否已提交（学生）
        if (userId != null) {
            vo.setSubmitted(checkSubmitted(e.getId(), userId));
        }

        return vo;
    }

    /**
     * 灵活解析日期，支持 "yyyy-MM-dd" 和 ISO-8601 ("2026-07-05T16:00:00.000Z") 两种格式
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        // 尝试普通日期格式 yyyy-MM-dd
        if (!dateStr.contains("T")) {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        // ISO-8601 格式：先解析为 Instant，再转为系统时区的 LocalDate
        Instant instant = Instant.parse(dateStr);
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
