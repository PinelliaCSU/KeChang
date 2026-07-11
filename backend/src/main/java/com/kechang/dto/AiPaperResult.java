package com.kechang.dto;

import com.kechang.entity.Question;
import lombok.Data;

import java.util.List;

/**
 * AI 组卷结果 DTO — 返回给前端预览
 */
@Data
public class AiPaperResult {

    /**
     * 建议的试卷名称
     */
    private String paperName;

    /**
     * 选题总分
     */
    private Double totalScore;

    /**
     * AI 组卷摘要说明
     */
    private String summary;

    /**
     * 候选题目总数
     */
    private Integer candidateCount;

    /**
     * 选中的题目列表（含建议分值）
     */
    private List<SuggestedQuestion> suggestedQuestions;

    @Data
    public static class SuggestedQuestion {

        private Long questionId;
        private String questionType;
        private String content;
        private String options;
        private String answer;
        private String analysis;
        private Integer difficulty;
        private String knowledgePoint;
        private Double questionScore;  // AI 建议的分值
        private String reason;         // AI 选题理由

        /**
         * 从 Question 实体构建 SuggestedQuestion
         */
        public static SuggestedQuestion fromQuestion(Question q, Double score, String reason) {
            SuggestedQuestion sq = new SuggestedQuestion();
            sq.setQuestionId(q.getId());
            sq.setQuestionType(q.getQuestionType());
            sq.setContent(q.getContent());
            sq.setOptions(q.getOptions());
            sq.setAnswer(q.getAnswer());
            sq.setAnalysis(q.getAnalysis());
            sq.setDifficulty(q.getDifficulty());
            sq.setKnowledgePoint(q.getKnowledgePoint());
            sq.setQuestionScore(score);
            sq.setReason(reason);
            return sq;
        }
    }
}
