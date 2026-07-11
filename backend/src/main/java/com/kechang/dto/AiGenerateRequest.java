package com.kechang.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 智能组卷请求 DTO
 */
@Data
public class AiGenerateRequest {

    /**
     * 所属课程 ID（必填）
     */
    private Long courseId;

    /**
     * 试卷名称（选填，留空由 AI 生成）
     */
    private String paperName;

    /**
     * 知识点关键词，逗号分隔
     */
    private String knowledgePoints;

    /**
     * 难度等级：1-容易 3-中等 5-困难，null=综合
     */
    private Integer difficulty;

    /**
     * 题型范围，逗号分隔，如 SINGLE,MULTI,JUDGE
     */
    private String types;

    /**
     * 期望题目总数
     */
    private Integer questionCount;

    /**
     * 目标总分
     */
    private Double totalScore;

    /**
     * 考试时长（分钟）
     */
    private Integer duration;

    /**
     * 及格分
     */
    private Double passScore;

    /**
     * 将 types 字符串解析为列表
     */
    public List<String> getTypeList() {
        if (types == null || types.isBlank()) {
            return List.of();
        }
        return List.of(types.split(","));
    }
}
