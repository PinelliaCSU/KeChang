package com.kechang.dto;

import lombok.Data;

import java.util.List;

@Data
public class ClassQuizCreateDTO {

    private Long courseId;
    private Long questionId;
    private List<Long> questionIds;
    private String title;
    private Integer durationSeconds;
}
