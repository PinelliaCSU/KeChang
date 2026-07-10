package com.kechang.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 签到记录
 */
@Data
@TableName("checkin_record")
public class CheckinRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private Long studentId;
    private LocalDateTime checkinTime;
    private String checkinMethod;  // 实际使用方式
    private BigDecimal latitude;   // 学生签到纬度
    private BigDecimal longitude;  // 学生签到经度
    private String status;         // SUCCESS / LATE / ABSENT

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
