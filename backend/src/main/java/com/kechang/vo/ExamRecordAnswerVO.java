package com.kechang.vo;

import lombok.Data;

@Data
public class ExamRecordAnswerVO {

    private Long id;
    private Long recordId;
    private Long questionId;
    private String questionType;
    private String questionContent;
    private String options;
    private String correctAnswer;
    private String analysis;
    private Double questionScore;
    private String studentAnswer;
    private Integer isCorrect;
    private Double score;
    private Long markedBy;
}
