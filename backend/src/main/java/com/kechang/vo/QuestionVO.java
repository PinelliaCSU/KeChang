package com.kechang.vo;

import lombok.Data;

/**
 * 试题 VO
 */
@Data
public class QuestionVO {

    private Long id;
    private String questionType;
    private String content;
    private String options;        // JSON
    private String answer;         // 学生端不返回
    private String analysis;
    private Double score;
    private Integer difficulty;
    private String knowledgePoint;
    private String programmingConfig;  // 编程题配置（JSON，学生端不含隐藏用例）
}
