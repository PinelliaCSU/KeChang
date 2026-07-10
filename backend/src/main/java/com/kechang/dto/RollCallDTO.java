package com.kechang.dto;

import lombok.Data;

@Data
public class RollCallDTO {

    private Long courseId;
    private Integer count;
    private Integer points;
    private String reason;
}
