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
}
