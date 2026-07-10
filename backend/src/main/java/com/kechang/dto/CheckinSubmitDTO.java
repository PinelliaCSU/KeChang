package com.kechang.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckinSubmitDTO {

    private String code;           // 数字码签到
    private BigDecimal latitude;   // 地理位置签到：学生纬度
    private BigDecimal longitude;  // 地理位置签到：学生经度
}
