package com.kechang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kechang.common.PageResult;
import com.kechang.entity.Notification;
import com.kechang.entity.NotificationUser;
import com.kechang.mapper.NotificationMapper;
import com.kechang.mapper.NotificationUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notifMapper;
    private final NotificationUserMapper notifUserMapper;

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
}
