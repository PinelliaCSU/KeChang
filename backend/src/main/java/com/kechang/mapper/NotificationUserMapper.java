package com.kechang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kechang.entity.NotificationUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificationUserMapper extends BaseMapper<NotificationUser> {

    /** 统计管理端平台公告的未读数（仅公告，不含作业/考试/实训等） */
    @Select("SELECT COUNT(*) FROM notification_user nu " +
            "INNER JOIN notification n ON nu.notification_id = n.id " +
            "WHERE nu.user_id = #{userId} AND nu.is_read = 0 AND nu.is_deleted = 0 " +
            "AND n.notification_type = '公告' AND n.is_deleted = 0 " +
            "AND (n.course_id IS NULL OR n.course_id = -1 OR n.course_id = -2)")
    Long countUnreadAnnouncements(@Param("userId") Long userId);

    /** 查询指定通知和用户的记录数（含软删除，绕过 @TableLogic） */
    @Select("SELECT COUNT(*) FROM notification_user WHERE notification_id = #{notifId} AND user_id = #{userId}")
    Long countByNotifIdAndUserIdAll(@Param("notifId") Long notifId, @Param("userId") Long userId);

    /** 插入软删除标记（绕过 @TableLogic，直接写入 is_deleted=1）。IGNORE 防止重复执行时报错回滚事务 */
    @org.apache.ibatis.annotations.Insert("INSERT IGNORE INTO notification_user (notification_id, user_id, is_read, is_deleted) VALUES (#{notifId}, #{userId}, 1, 1)")
    void insertDeletedMarker(@Param("notifId") Long notifId, @Param("userId") Long userId);

    /** 统计某通知的确认人数（含已软删除的记录，绕过 @TableLogic） */
    @Select("SELECT COUNT(*) FROM notification_user WHERE notification_id = #{notifId} AND is_read = 1")
    Long countConfirmations(@Param("notifId") Long notifId);

    /** 查询指定通知和用户的记录（含软删除，绕过 @TableLogic） */
    @Select("SELECT * FROM notification_user WHERE notification_id = #{notifId} AND user_id = #{userId}")
    NotificationUser selectByNotifIdAndUserIdAll(@Param("notifId") Long notifId, @Param("userId") Long userId);
}
