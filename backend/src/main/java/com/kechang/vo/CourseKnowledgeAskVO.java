package com.kechang.vo;

import lombok.Data;

import java.util.List;

@Data
public class CourseKnowledgeAskVO {

    private String answer;
    private List<Reference> references;

    @Data
    public static class Reference {
        private Long chunkId;
        private Long documentId;
        private String title;
        private String sourceType;
        private String sourceUrl;
        private Double score;
        private String snippet;
    }
}
