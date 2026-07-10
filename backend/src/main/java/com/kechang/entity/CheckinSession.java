package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 签到场次
 */
@Data
@TableName("checkin_session")
public class CheckinSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private String title;
    private String checkinType;   // QR_CODE / NUMERIC_CODE / LOCATION
    private String code;           // 6位数字签到码
    private BigDecimal latitude;   // 地理位置纬度
    private BigDecimal longitude;  // 地理位置经度
    private Integer locationRadius; // 有效半径（米）
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;         // ACTIVE / ENDED
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
