package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.dto.AnnouncementDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知服务（含公告功能）
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notifMapper;
    private final NotificationUserMapper notifUserMapper;
    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final SysUserMapper sysUserMapper;
    private final StudentProfileMapper studentProfileMapper;

    // ==================== 通用通知 ====================

    public PageResult<Notification> list(Long userId, int page, int size) {
        List<NotificationUser> nus = notifUserMapper.selectList(
                new LambdaQueryWrapper<NotificationUser>().eq(NotificationUser::getUserId, userId));
        if (nus.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        List<Long> ids = nus.stream().map(NotificationUser::getNotificationId).toList();
        Page<Notification> p = notifMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>().in(Notification::getId, ids)
                        .orderByDesc(Notification::getSendTime));
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public long unreadCount(Long userId) {
        return notifUserMapper.selectCount(new LambdaQueryWrapper<NotificationUser>()
                .eq(NotificationUser::getUserId, userId).eq(NotificationUser::getIsRead, 0));
    }

    public void markRead(Long id, Long userId) {
        NotificationUser nu = notifUserMapper.selectOne(new LambdaQueryWrapper<NotificationUser>()
                .eq(NotificationUser::getNotificationId, id).eq(NotificationUser::getUserId, userId));
        if (nu != null) {
            nu.setIsRead(1);
            nu.setReadTime(LocalDateTime.now());
            notifUserMapper.updateById(nu);
        }
    }

    public void markAllRead(Long userId) {
        List<NotificationUser> nus = notifUserMapper.selectList(new LambdaQueryWrapper<NotificationUser>()
                .eq(NotificationUser::getUserId, userId).eq(NotificationUser::getIsRead, 0));
        nus.forEach(nu -> {
            nu.setIsRead(1);
            nu.setReadTime(LocalDateTime.now());
            notifUserMapper.updateById(nu);
        });
    }

    public void send(Notification n) {
        notifMapper.insert(n);
    }

    // ==================== 公告功能 ====================

    /**
     * 分页查询公告列表
     * - 教师：查看自己发布的公告
     * - 学生：查看已选课程的公告，附带确认状态
     */
    public PageResult<Map<String, Object>> listAnnouncements(int page, int size, Long courseId, Long userId,
            String role) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getNotificationType, "公告");

        if ("ROLE_TEACHER".equals(role)) {
            // 教师只看自己的公告
            wrapper.eq(Notification::getSenderId, userId);
        } else {
            // 学生只看已选课程的公告
            List<CourseStudent> enrolled = courseStudentMapper.selectList(
                    new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getStudentId, userId));
            if (enrolled.isEmpty()) {
                return PageResult.of(List.of(), 0, page, size);
            }
            List<Long> courseIds = enrolled.stream().map(CourseStudent::getCourseId).distinct().toList();
            wrapper.in(Notification::getCourseId, courseIds);
        }

        if (courseId != null) {
            wrapper.eq(Notification::getCourseId, courseId);
        }
        wrapper.orderByDesc(Notification::getSendTime);

        Page<Notification> p = notifMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Notification n : p.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", n.getId());
            item.put("title", n.getTitle());
            item.put("content", n.getContent());
            item.put("courseId", n.getCourseId());
            item.put("publisherId", n.getSenderId());
            item.put("publishTime", n.getSendTime());
            item.put("createTime", n.getCreateTime());

            // 课程名
            if (n.getCourseId() != null) {
                Course c = courseMapper.selectById(n.getCourseId());
                item.put("courseName", c != null ? c.getCourseName() : "");
            } else {
                item.put("courseName", "");
            }

            // 发布教师名
            if (n.getSenderId() != null) {
                SysUser u = sysUserMapper.selectById(n.getSenderId());
                item.put("publisherName", u != null ? u.getNickname() : "");
            } else {
                item.put("publisherName", "");
            }

            // 确认人数
            long confirmCount = notifUserMapper.selectCount(new LambdaQueryWrapper<NotificationUser>()
                    .eq(NotificationUser::getNotificationId, n.getId())
                    .eq(NotificationUser::getIsRead, 1));
            item.put("confirmCount", (int) confirmCount);

            // 当前学生是否已确认
            if ("ROLE_STUDENT".equals(role)) {
                NotificationUser nu = notifUserMapper.selectOne(new LambdaQueryWrapper<NotificationUser>()
                        .eq(NotificationUser::getNotificationId, n.getId())
                        .eq(NotificationUser::getUserId, userId));
                item.put("confirmed", nu != null && nu.getIsRead() != null && nu.getIsRead() == 1);
            } else {
                item.put("confirmed", false);
            }

            records.add(item);
        }
        return PageResult.of(records, p.getTotal(), page, size);
    }

    /**
     * 获取公告详情
     */
    public Map<String, Object> getAnnouncementDetail(Long id, Long userId, String role) {
        Notification n = notifMapper.selectById(id);
        if (n == null || !"公告".equals(n.getNotificationType())) {
            throw BusinessException.notFound("公告不存在");
        }

        Map<String, Object> item = new HashMap<>();
        item.put("id", n.getId());
        item.put("title", n.getTitle());
        item.put("content", n.getContent());
        item.put("courseId", n.getCourseId());
        item.put("publisherId", n.getSenderId());
        item.put("publishTime", n.getSendTime());
        item.put("createTime", n.getCreateTime());

        if (n.getCourseId() != null) {
            Course c = courseMapper.selectById(n.getCourseId());
            item.put("courseName", c != null ? c.getCourseName() : "");
        } else {
            item.put("courseName", "");
        }

        if (n.getSenderId() != null) {
            SysUser u = sysUserMapper.selectById(n.getSenderId());
            item.put("publisherName", u != null ? u.getNickname() : "");
        } else {
            item.put("publisherName", "");
        }

        long confirmCount = notifUserMapper.selectCount(new LambdaQueryWrapper<NotificationUser>()
                .eq(NotificationUser::getNotificationId, n.getId())
                .eq(NotificationUser::getIsRead, 1));
        item.put("confirmCount", (int) confirmCount);

        if ("ROLE_STUDENT".equals(role)) {
            NotificationUser nu = notifUserMapper.selectOne(new LambdaQueryWrapper<NotificationUser>()
                    .eq(NotificationUser::getNotificationId, n.getId())
                    .eq(NotificationUser::getUserId, userId));
            item.put("confirmed", nu != null && nu.getIsRead() != null && nu.getIsRead() == 1);
        } else {
            item.put("confirmed", false);
        }

        // 权限校验：学生只能看已选课程的公告
        if ("ROLE_STUDENT".equals(role) && n.getCourseId() != null) {
            CourseStudent cs = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, n.getCourseId())
                    .eq(CourseStudent::getStudentId, userId));
            if (cs == null) {
                throw BusinessException.forbidden("您未选修该课程，无法查看此公告");
            }
        }

        return item;
    }

    /**
     * 发布公告：插入 notification + 为课程所有学生创建 notification_user 记录
     */
    @Transactional
    public void createAnnouncement(AnnouncementDTO dto, Long teacherId) {
        Notification n = new Notification();
        n.setTitle(dto.getTitle());
        n.setContent(dto.getContent());
        n.setCourseId(dto.getCourseId());
        n.setSenderId(teacherId);
        n.setSendTime(LocalDateTime.now());
        n.setNotificationType("公告");
        notifMapper.insert(n);

        // 为课程所有学生创建 notification_user 记录
        List<CourseStudent> students = courseStudentMapper.selectList(
                new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getCourseId, dto.getCourseId()));
        for (CourseStudent cs : students) {
            NotificationUser nu = new NotificationUser();
            nu.setNotificationId(n.getId());
            nu.setUserId(cs.getStudentId());
            nu.setIsRead(0);
            notifUserMapper.insert(nu);
        }
    }

    /**
     * 编辑公告（仅发布者本人可编辑）
     */
    public void updateAnnouncement(Long id, AnnouncementDTO dto, Long userId) {
        Notification n = notifMapper.selectById(id);
        if (n == null || !"公告".equals(n.getNotificationType())) {
            throw BusinessException.notFound("公告不存在");
        }
        if (!n.getSenderId().equals(userId)) {
            throw BusinessException.forbidden("只能编辑自己发布的公告");
        }
        n.setTitle(dto.getTitle());
        n.setContent(dto.getContent());
        n.setCourseId(dto.getCourseId());
        notifMapper.updateById(n);
    }

    /**
     * 删除公告（逻辑删除，仅发布者本人可删）
     */
    @Transactional
    public void deleteAnnouncement(Long id, Long userId) {
        Notification n = notifMapper.selectById(id);
        if (n == null || !"公告".equals(n.getNotificationType())) {
            throw BusinessException.notFound("公告不存在");
        }
        if (!n.getSenderId().equals(userId)) {
            throw BusinessException.forbidden("只能删除自己发布的公告");
        }
        notifMapper.deleteById(id);

        // 同时删除关联的 notification_user 记录
        List<NotificationUser> nus = notifUserMapper.selectList(
                new LambdaQueryWrapper<NotificationUser>().eq(NotificationUser::getNotificationId, id));
        for (NotificationUser nu : nus) {
            notifUserMapper.deleteById(nu.getId());
        }
    }

    /**
     * 学生确认公告
     */
    public void confirmAnnouncement(Long announcementId, Long studentId) {
        Notification n = notifMapper.selectById(announcementId);
        if (n == null || !"公告".equals(n.getNotificationType())) {
            throw BusinessException.notFound("公告不存在");
        }

        // 校验学生已选该课程
        if (n.getCourseId() != null) {
            CourseStudent cs = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, n.getCourseId())
                    .eq(CourseStudent::getStudentId, studentId));
            if (cs == null) {
                throw BusinessException.forbidden("您未选修该课程，无法确认此公告");
            }
        }

        // 查找或创建 notification_user 记录
        NotificationUser nu = notifUserMapper.selectOne(new LambdaQueryWrapper<NotificationUser>()
                .eq(NotificationUser::getNotificationId, announcementId)
                .eq(NotificationUser::getUserId, studentId));
        if (nu == null) {
            // 如果该学生还没有记录（可能是在创建公告后才选课），则创建一条
            nu = new NotificationUser();
            nu.setNotificationId(announcementId);
            nu.setUserId(studentId);
            nu.setIsRead(1);
            nu.setReadTime(LocalDateTime.now());
            notifUserMapper.insert(nu);
        } else if (nu.getIsRead() != null && nu.getIsRead() == 1) {
            throw BusinessException.badRequest("您已确认过该公告");
        } else {
            nu.setIsRead(1);
            nu.setReadTime(LocalDateTime.now());
            notifUserMapper.updateById(nu);
        }
    }

    /**
     * 教师查看公告确认列表（含学生姓名学号）
     */
    public PageResult<Map<String, Object>> getConfirmList(Long announcementId, int page, int size, Long userId) {
        Notification n = notifMapper.selectById(announcementId);
        if (n == null || !"公告".equals(n.getNotificationType())) {
            throw BusinessException.notFound("公告不存在");
        }
        if (!n.getSenderId().equals(userId)) {
            throw BusinessException.forbidden("只能查看自己发布的公告的确认情况");
        }

        Page<NotificationUser> p = notifUserMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<NotificationUser>()
                        .eq(NotificationUser::getNotificationId, announcementId)
                        .eq(NotificationUser::getIsRead, 1)
                        .orderByAsc(NotificationUser::getReadTime));

        List<Map<String, Object>> records = new ArrayList<>();
        for (NotificationUser nu : p.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", nu.getId());
            item.put("studentId", nu.getUserId());
            item.put("confirmTime", nu.getReadTime());

            // 学生信息
            SysUser u = sysUserMapper.selectById(nu.getUserId());
            item.put("studentName", u != null ? u.getNickname() : "");
            item.put("username", u != null ? u.getUsername() : "");

            StudentProfile sp = studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, nu.getUserId()));
            item.put("studentNo", sp != null ? sp.getStudentNo() : "");

            records.add(item);
        }
        return PageResult.of(records, p.getTotal(), page, size);
    }

    /**
     * 获取公告的确认人数
     */
    public int getConfirmCount(Long announcementId) {
        return (int) notifUserMapper.selectCount(new LambdaQueryWrapper<NotificationUser>()
                .eq(NotificationUser::getNotificationId, announcementId)
                .eq(NotificationUser::getIsRead, 1));
    }
}
