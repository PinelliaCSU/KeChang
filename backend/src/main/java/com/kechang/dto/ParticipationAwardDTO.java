package com.kechang.dto;

import lombok.Data;

@Data
public class ParticipationAwardDTO {

    private Long courseId;
    private Long studentId;
    private Integer points;
    private String reason;
}
