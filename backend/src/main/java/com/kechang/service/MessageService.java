package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.BusinessException;
import com.kechang.common.PageResult;
import com.kechang.dto.SendMessageDTO;
import com.kechang.entity.*;
import com.kechang.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 私信服务（复用 notification + notification_user 表，type='私信'）
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final NotificationMapper notifMapper;
    private final NotificationUserMapper notifUserMapper;
    private final SysUserMapper sysUserMapper;
    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final StudentProfileMapper studentProfileMapper;

    // ==================== 会话列表 ====================
    /**
     * 获取当前用户的会话列表（按最新消息时间降序）
     */
    public PageResult<Map<String, Object>> listConversations(Long userId, int page, int size) {
        // 1. 收集所有有过私信往来的 partner ID
        Set<Long> partnerIdSet = new LinkedHashSet<>(); // 保持插入顺序（按时间降序需要后面排序）

        // 1a. 我发给别人的消息（排除我已删除的）→ partner = 接收方的 user_id
        List<Notification> sentMsgs = notifMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getSenderId, userId)
                        .eq(Notification::getNotificationType, "私信")
                        .apply("id NOT IN (SELECT notification_id FROM notification_user WHERE user_id = {0} AND is_deleted = 1)", userId)
                        .select(Notification::getId));
        if (!sentMsgs.isEmpty()) {
            List<Long> sentIds = sentMsgs.stream().map(Notification::getId).toList();
            List<NotificationUser> nus = notifUserMapper.selectList(
                    new LambdaQueryWrapper<NotificationUser>()
                            .in(NotificationUser::getNotificationId, sentIds));
            for (NotificationUser nu : nus) {
                if (!nu.getUserId().equals(userId)) {
                    partnerIdSet.add(nu.getUserId());
                }
            }
        }

        // 1b. 别人发给我的消息 → partner = notification.sender_id
        List<NotificationUser> recvNus = notifUserMapper.selectList(
                new LambdaQueryWrapper<NotificationUser>()
                        .eq(NotificationUser::getUserId, userId));
        if (!recvNus.isEmpty()) {
            List<Long> recvNotifIds = recvNus.stream().map(NotificationUser::getNotificationId).toList();
            List<Notification> recvMsgs = notifMapper.selectList(
                    new LambdaQueryWrapper<Notification>()
                            .in(Notification::getId, recvNotifIds)
                            .eq(Notification::getNotificationType, "私信"));
            for (Notification n : recvMsgs) {
                partnerIdSet.add(n.getSenderId());
            }
        }

        List<Long> partnerIds = new ArrayList<>(partnerIdSet);

        // 2. 为每个 partner 构建会话摘要（最新消息 + 未读数 + 最新时间）
        List<Map<String, Object>> conversations = new ArrayList<>();
        for (Long partnerId : partnerIds) {
            // 找双方之间最新的一条消息（我发的 或 对方发的）
            Notification latestSent = notifMapper.selectOne(
                    new LambdaQueryWrapper<Notification>()
                            .eq(Notification::getSenderId, userId)
                            .eq(Notification::getNotificationType, "私信")
                            .apply("id IN (SELECT notification_id FROM notification_user WHERE user_id = {0})",
                                    partnerId)
                            .orderByDesc(Notification::getSendTime)
                            .last("LIMIT 1"));

            Notification latestRecv = notifMapper.selectOne(
                    new LambdaQueryWrapper<Notification>()
                            .eq(Notification::getSenderId, partnerId)
                            .eq(Notification::getNotificationType, "私信")
                            .apply("id IN (SELECT notification_id FROM notification_user WHERE user_id = {0})",
                                    userId)
                            .orderByDesc(Notification::getSendTime)
                            .last("LIMIT 1"));

            Notification latest = null;
            if (latestSent != null && latestRecv != null) {
                latest = latestSent.getSendTime().isAfter(latestRecv.getSendTime()) ? latestSent : latestRecv;
            } else {
                latest = latestSent != null ? latestSent : latestRecv;
            }
            if (latest == null) {
                continue;
            }

            // 未读数：对方发给我且我未读的消息数
            long unread = 0;
            if (latestRecv != null) {
                unread = notifUserMapper.selectCount(
                        new LambdaQueryWrapper<NotificationUser>()
                                .eq(NotificationUser::getUserId, userId)
                                .eq(NotificationUser::getIsRead, 0)
                                .apply("notification_id IN (SELECT id FROM notification WHERE sender_id = {0}"
                                        + " AND notification_type = '私信' AND is_deleted = 0)",
                                        partnerId));
            }

            // 对方用户信息
            SysUser partner = sysUserMapper.selectById(partnerId);

            Map<String, Object> conv = new HashMap<>();
            conv.put("partnerId", partnerId);
            conv.put("partnerName", getUserDisplayName(partnerId));
            conv.put("partnerAvatar", partner != null ? partner.getAvatar() : "");
            conv.put("partnerUsername", partner != null ? partner.getUsername() : "");
            conv.put("lastContent", latest.getContent());
            conv.put("lastTime", latest.getSendTime());
            conv.put("lastIsMine", latest.getSenderId().equals(userId));
            conv.put("unreadCount", (int) unread);
            conversations.add(conv);
        }

        // 3. 按最新消息时间降序排序
        conversations.sort((a, b) -> {
            LocalDateTime ta = (LocalDateTime) a.get("lastTime");
            LocalDateTime tb = (LocalDateTime) b.get("lastTime");
            if (ta == null && tb == null) {
                return 0;
            }
            if (ta == null) {
                return 1;
            }
            if (tb == null) {
                return -1;
            }
            return tb.compareTo(ta);
        });

        // 4. 手动分页
        int total = conversations.size();
        int fromIdx = (page - 1) * size;
        int toIdx = Math.min(fromIdx + size, total);
        List<Map<String, Object>> paged = fromIdx < total ? conversations.subList(fromIdx, toIdx) : List.of();

        return PageResult.of(paged, total, page, size);
    }

    // ==================== 聊天记录 ====================
    /**
     * 获取与某用户的聊天记录，并自动标记已读
     */
    @Transactional
    public PageResult<Map<String, Object>> getMessages(Long userId, Long otherUserId, int page, int size) {
        // 1. 查询双方之间的所有消息（分页，按发送时间降序）
        // 排除当前用户已删除的消息：w1(我发的)检查我的 notification_user 未被软删除；w2(发给我的)检查我的 notification_user 未被软删除
        Page<Notification> p = notifMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getNotificationType, "私信")
                        .and(w -> w
                        .and(w1 -> w1.eq(Notification::getSenderId, userId)
                        .apply("id IN (SELECT notification_id FROM notification_user WHERE user_id = {0})", otherUserId)
                        .apply("id NOT IN (SELECT notification_id FROM notification_user WHERE user_id = {0} AND is_deleted = 1)", userId))
                        .or(w2 -> w2.eq(Notification::getSenderId, otherUserId)
                        .apply("id IN (SELECT notification_id FROM notification_user WHERE user_id = {0} AND is_deleted = 0)",
                                userId)))
                        .orderByDesc(Notification::getSendTime));

        // 2. 自动标记已读（对方发给我的未读消息）
        List<NotificationUser> unreadList = notifUserMapper.selectList(
                new LambdaQueryWrapper<NotificationUser>()
                        .eq(NotificationUser::getUserId, userId)
                        .eq(NotificationUser::getIsRead, 0)
                        .apply("notification_id IN (SELECT id FROM notification WHERE sender_id = {0}"
                                + " AND notification_type = '私信' AND is_deleted = 0)",
                                otherUserId));
        for (NotificationUser nu : unreadList) {
            nu.setIsRead(1);
            nu.setReadTime(LocalDateTime.now());
            notifUserMapper.updateById(nu);
        }

        // 3. 组装返回数据
        List<Map<String, Object>> records = new ArrayList<>();
        for (Notification n : p.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", n.getId());
            item.put("senderId", n.getSenderId());
            item.put("content", n.getContent());
            item.put("sendTime", n.getSendTime());
            item.put("isMine", n.getSenderId().equals(userId));

            // 发送者信息
            SysUser sender = sysUserMapper.selectById(n.getSenderId());
            item.put("senderName", getUserDisplayName(n.getSenderId()));
            item.put("senderAvatar", sender != null ? sender.getAvatar() : "");

            // 已读状态：查接收方的 notification_user.is_read
            if (!n.getSenderId().equals(userId)) {
                // 对方发给我的 → 查我的已读状态
                NotificationUser nu = notifUserMapper.selectOne(
                        new LambdaQueryWrapper<NotificationUser>()
                                .eq(NotificationUser::getNotificationId, n.getId())
                                .eq(NotificationUser::getUserId, userId));
                item.put("isRead", nu != null && nu.getIsRead() != null ? nu.getIsRead() : 0);
            } else {
                // 我发给对方的 → 查对方的已读状态（绕过 @TableLogic，对方可能已删除但仍需显示已读）
                NotificationUser nu = notifUserMapper.selectByNotifIdAndUserIdAll(n.getId(), otherUserId);
                item.put("isRead", nu != null && nu.getIsRead() != null ? nu.getIsRead() : 0);
            }

            records.add(item);
        }

        return PageResult.of(records, p.getTotal(), page, size);
    }

    // ==================== 发送消息 ====================
    /**
     * 发送私信
     */
    @Transactional
    public Map<String, Object> sendMessage(Long senderId, SendMessageDTO dto) {
        // 校验接收者存在
        SysUser receiver = sysUserMapper.selectById(dto.getReceiverId());
        if (receiver == null) {
            throw BusinessException.notFound("接收者不存在");
        }

        // 创建通知记录（作为消息）
        Notification n = new Notification();
        n.setTitle("私信");
        n.setContent(dto.getContent());
        n.setSenderId(senderId);
        n.setSendTime(LocalDateTime.now());
        n.setNotificationType("私信");
        notifMapper.insert(n);

        // 为接收者创建 notification_user 记录
        NotificationUser nu = new NotificationUser();
        nu.setNotificationId(n.getId());
        nu.setUserId(dto.getReceiverId());
        nu.setIsRead(0);
        notifUserMapper.insert(nu);

        // 组装返回
        SysUser sender = sysUserMapper.selectById(senderId);
        Map<String, Object> result = new HashMap<>();
        result.put("id", n.getId());
        result.put("senderId", senderId);
        result.put("senderName", getUserDisplayName(n.getSenderId()));
        result.put("senderAvatar", sender != null ? sender.getAvatar() : "");
        result.put("content", n.getContent());
        result.put("sendTime", n.getSendTime());
        result.put("isMine", true);
        result.put("isRead", 0);
        return result;
    }

    // ==================== 未读总数 ====================
    /**
     * 获取当前用户私信未读总数
     */
    public long unreadTotal(Long userId) {
        List<NotificationUser> unreadNus = notifUserMapper.selectList(
                new LambdaQueryWrapper<NotificationUser>()
                        .eq(NotificationUser::getUserId, userId)
                        .eq(NotificationUser::getIsRead, 0));
        if (unreadNus.isEmpty()) {
            return 0;
        }

        List<Long> notifIds = unreadNus.stream().map(NotificationUser::getNotificationId).toList();
        return notifMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .in(Notification::getId, notifIds)
                        .eq(Notification::getNotificationType, "私信"));
    }

    // ==================== 联系人 ====================
    /**
     * 获取联系人列表（课程关联 + 自由搜索）
     */
    public List<Map<String, Object>> getContacts(Long userId, String role, String keyword) {
        Set<Long> seenIds = new HashSet<>();
        List<Map<String, Object>> contacts = new ArrayList<>();

        // 来源1：课程关联联系人
        if ("ROLE_TEACHER".equals(role)) {
            // 教师 → 自己所有课程的学生
            List<Course> courses = courseMapper.selectList(
                    new LambdaQueryWrapper<Course>().eq(Course::getTeacherId, userId));
            for (Course c : courses) {
                List<CourseStudent> students = courseStudentMapper.selectList(
                        new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getCourseId, c.getId()));
                for (CourseStudent cs : students) {
                    if (seenIds.add(cs.getStudentId())) {
                        SysUser u = sysUserMapper.selectById(cs.getStudentId());
                        if (u != null) {
                            StudentProfile sp = studentProfileMapper.selectOne(
                                    new LambdaQueryWrapper<StudentProfile>()
                                            .eq(StudentProfile::getUserId, cs.getStudentId()));
                            Map<String, Object> contact = new HashMap<>();
                            contact.put("userId", u.getId());
                            contact.put("name",
                                    sp != null && sp.getRealName() != null ? sp.getRealName() : u.getNickname());
                            contact.put("avatar", u.getAvatar());
                            contact.put("username", u.getUsername());
                            contact.put("type", "student");
                            contact.put("courseName", c.getCourseName());
                            contacts.add(contact);
                        }
                    }
                }
            }
        } else if ("ROLE_STUDENT".equals(role)) {
            // 学生 → 自己所有课程的授课教师
            List<CourseStudent> enrollments = courseStudentMapper.selectList(
                    new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getStudentId, userId));
            for (CourseStudent cs : enrollments) {
                Course c = courseMapper.selectById(cs.getCourseId());
                if (c != null && seenIds.add(c.getTeacherId())) {
                    SysUser u = sysUserMapper.selectById(c.getTeacherId());
                    if (u != null) {
                        TeacherProfile tp = teacherProfileMapper.selectOne(
                                new LambdaQueryWrapper<TeacherProfile>()
                                        .eq(TeacherProfile::getUserId, c.getTeacherId()));
                        Map<String, Object> contact = new HashMap<>();
                        contact.put("userId", u.getId());
                        contact.put("name",
                                tp != null && tp.getRealName() != null ? tp.getRealName() : u.getNickname());
                        contact.put("avatar", u.getAvatar());
                        contact.put("username", u.getUsername());
                        contact.put("type", "teacher");
                        contact.put("courseName", c.getCourseName());
                        contacts.add(contact);
                    }
                }
            }
        }

        // 关键词过滤：课程关联联系人也需要按关键词筛选
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            contacts.removeIf(c -> {
                String name = String.valueOf(c.getOrDefault("name", "")).toLowerCase();
                String uname = String.valueOf(c.getOrDefault("username", "")).toLowerCase();
                return !name.contains(kw) && !uname.contains(kw);
            });
        }

        // 来源2：自由搜索（按用户名/昵称，排除已在课程联系人中的用户）
        if (keyword != null && !keyword.isBlank()) {
            List<SysUser> searched = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .ne(SysUser::getId, userId)
                            .and(w -> w.like(SysUser::getUsername, keyword)
                            .or().like(SysUser::getNickname, keyword))
                            .last("LIMIT 20"));
            for (SysUser u : searched) {
                if (seenIds.add(u.getId())) {
                    Map<String, Object> contact = new HashMap<>();
                    contact.put("userId", u.getId());
                    contact.put("name", u.getNickname());
                    contact.put("avatar", u.getAvatar());
                    contact.put("username", u.getUsername());
                    contact.put("type", "");
                    contact.put("courseName", "");
                    contacts.add(contact);
                }
            }
        }

        return contacts;
    }

    // ==================== 清空聊天记录 ====================
    /**
     * 清空当前用户与某人的聊天记录（软删除）
     */
    @Transactional
    public void clearConversation(Long userId, Long otherUserId) {
        // 1. 找到双方之间所有私信通知
        List<Notification> notifications = notifMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getNotificationType, "私信")
                        .and(w -> w
                        .and(w1 -> w1.eq(Notification::getSenderId, userId)
                        .apply("id IN (SELECT notification_id FROM notification_user WHERE user_id = {0})",
                                otherUserId))
                        .or(w2 -> w2.eq(Notification::getSenderId, otherUserId)
                        .apply("id IN (SELECT notification_id FROM notification_user WHERE user_id = {0} AND is_deleted = 0)",
                                userId))));

        if (notifications.isEmpty()) {
            return;
        }

        List<Long> notifIds = notifications.stream().map(Notification::getId).toList();

        // 2. 删除当前用户已有的 notification_user 记录（接收的消息 + 新发送的消息）
        List<NotificationUser> nus = notifUserMapper.selectList(
                new LambdaQueryWrapper<NotificationUser>()
                        .in(NotificationUser::getNotificationId, notifIds)
                        .eq(NotificationUser::getUserId, userId));
        for (NotificationUser nu : nus) {
            notifUserMapper.deleteById(nu.getId());
        }

        // 3. 为没有 notification_user 记录的消息（用户发送的）插入软删除标记
        for (Notification n : notifications) {
            if (n.getSenderId().equals(userId)
                    && notifUserMapper.countByNotifIdAndUserIdAll(n.getId(), userId) == 0) {
                notifUserMapper.insertDeletedMarker(n.getId(), userId);
            }
        }
    }

    // ==================== 辅助方法 ====================

    /** 获取用户真实姓名，优先 StudentProfile，其次 TeacherProfile，最后回退到昵称 */
    private String getUserDisplayName(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) return "";

        // 尝试学生档案
        StudentProfile sp = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, userId));
        if (sp != null && sp.getRealName() != null && !sp.getRealName().isBlank()) {
            return sp.getRealName();
        }

        // 尝试教师档案
        TeacherProfile tp = teacherProfileMapper.selectOne(
                new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, userId));
        if (tp != null && tp.getRealName() != null && !tp.getRealName().isBlank()) {
            return tp.getRealName();
        }

        // 回退到昵称
        return user.getNickname() != null ? user.getNickname() : "";
    }
}
