package com.kechang.vo;

import lombok.Data;

@Data
public class CourseKnowledgeStatsVO {

    private Long courseId;
    private String courseName;
    private Long documentCount;
    private Long chunkCount;
    private Integer readyDocumentCount;
    private Integer failedDocumentCount;
    private Long sourceResourceCount;
    private Long sourceAssignmentCount;
    private Long sourceQuestionCount;
    private Long sourceQaCount;
    private Boolean storageReady;
}
