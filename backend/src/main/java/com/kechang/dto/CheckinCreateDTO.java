package com.kechang.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckinCreateDTO {

    private Long courseId;
    private String title;
    private String checkinType;     // QR_CODE / NUMERIC_CODE / LOCATION
    private BigDecimal latitude;    // LOCATION方式：教师设定纬度
    private BigDecimal longitude;   // LOCATION方式：教师设定经度
    private Integer locationRadius; // LOCATION方式：有效半径（米），默认100
    private Integer durationMinutes; // 持续分钟数，默认5
}
