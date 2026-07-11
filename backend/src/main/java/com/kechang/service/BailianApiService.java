package com.kechang.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kechang.config.BailianConfig;
import com.kechang.dto.AiPaperResult;
import com.kechang.entity.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 阿里百炼（DashScope）API 服务 调用千问大模型实现智能组卷
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BailianApiService {

    private final BailianConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用 AI 生成组卷方案
     *
     * @param candidates 候选题目列表
     * @param knowledgePoints 知识点要求
     * @param difficulty 难度要求
     * @param typeList 题型列表
     * @param questionCount 期望题数
     * @param targetScore 目标总分
     * @return AI 组卷结果
     */
    public AiPaperResult generatePaper(List<Question> candidates,
            String knowledgePoints,
            Integer difficulty,
            List<String> typeList,
            Integer questionCount,
            Double targetScore) {

        // 构建 candidate JSON（只发送必要字段，减少 token 消耗）
        List<Map<String, Object>> candidateJson = candidates.stream().map(q -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("type", q.getQuestionType());
            m.put("content", q.getContent());
            m.put("difficulty", q.getDifficulty());
            m.put("knowledgePoint", q.getKnowledgePoint());
            m.put("defaultScore", q.getScore());
            return m;
        }).collect(Collectors.toList());

        // 构建提示词
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(candidateJson, knowledgePoints, difficulty, typeList, questionCount, targetScore);

        // 调用 API
        String aiResponse = callChatApi(systemPrompt, userPrompt);

        // 解析返回结果
        return parseAiResponse(aiResponse, candidates);
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return """
            你是一位资深的试卷设计专家。你的任务是根据提供的题库和组卷要求，选择最合适的题目组成一份高质量的试卷。

            组卷原则：
            1. 知识点覆盖：尽量覆盖不同的知识点，避免集中在单一知识点
            2. 难度梯度：合理分布难度（容易→中等→困难），呈正态分布
            3. 题型搭配：根据要求合理分配各题型数量
            4. 总分控制：选题分值之和尽量接近目标总分
            5. 题目质量：优先选择表述清晰、考察核心概念的题目

            你必须严格从提供的题库中选择题目，不得编造。以 JSON 格式返回结果。
            """;
    }

    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(List<Map<String, Object>> candidates,
            String knowledgePoints, Integer difficulty,
            List<String> typeList, Integer questionCount, Double targetScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下要求从题库中选择题目组卷：\n\n");

        sb.append("## 组卷要求\n");
        if (knowledgePoints != null && !knowledgePoints.isBlank()) {
            sb.append("- 知识点覆盖：").append(knowledgePoints).append("\n");
        }
        if (difficulty != null) {
            String diffLabel = difficulty <= 2 ? "容易" : difficulty <= 3 ? "中等" : "困难";
            sb.append("- 难度倾向：").append(diffLabel).append("（难度值 ").append(difficulty).append("）\n");
        } else {
            sb.append("- 难度倾向：综合（各难度合理搭配）\n");
        }
        if (!typeList.isEmpty()) {
            sb.append("- 题型范围：仅限 ").append(String.join("、", typeList)).append("\n");
        }
        if (questionCount != null && questionCount > 0) {
            sb.append("- 选题数量：共 ").append(questionCount).append(" 题\n");
        }
        if (targetScore != null && targetScore > 0) {
            sb.append("- 目标总分：").append(targetScore).append(" 分（各题分值之和应接近此值）\n");
        }
        sb.append("\n");

        sb.append("## 候选题库（共 ").append(candidates.size()).append(" 题）\n");
        try {
            sb.append(objectMapper.writeValueAsString(candidates));
        } catch (JsonProcessingException e) {
            sb.append("[题库序列化失败]");
        }
        sb.append("\n\n");

        sb.append("## 输出要求\n");
        sb.append("请以 JSON 格式返回选题结果，格式如下：\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"paperName\": \"建议的试卷名称\",\n");
        sb.append("  \"summary\": \"组卷说明（100字以内，说明选题思路和覆盖情况）\",\n");
        sb.append("  \"questions\": [\n");
        sb.append("    {\"questionId\": 1, \"questionScore\": 10, \"reason\": \"选题理由（简短）\"},\n");
        sb.append("    ...\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("```\n");
        sb.append("\n注意：questionId 必须来自上述题库中的 id，questionScore 为建议分值，reason 为简短选题理由。");

        return sb.toString();
    }

    /**
     * 调用阿里百炼 Chat Completions API（JSON 模式，用于组卷）
     */
    private String callChatApi(String systemPrompt, String userPrompt) {
        return callChatApiInternal(systemPrompt, userPrompt, true);
    }

    /**
     * 调用阿里百炼 Chat Completions API（文本模式，用于摘要等自由输出）
     */
    private String callChatApiText(String systemPrompt, String userPrompt) {
        return callChatApiInternal(systemPrompt, userPrompt, false);
    }

    /**
     * 调用阿里百炼 Chat Completions API
     */
    private String callChatApiInternal(String systemPrompt, String userPrompt, boolean jsonMode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + config.getApiKey());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModel());

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);

        // 控制输出格式和随机性
        body.put("temperature", 0.3);
        body.put("max_tokens", 4096);
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.info("Calling Bailian API, model={}, candidates count in prompt", config.getModel());
            ResponseEntity<Map> response = restTemplate.postForEntity(config.getEndpoint(), request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Bailian API returned error: " + response.getStatusCode());
            }

            // 解析 OpenAI 兼容格式的返回
            Map<String, Object> respBody = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("Bailian API returned empty choices");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            log.info("Bailian API response received, length={}", content != null ? content.length() : 0);
            return content;

        } catch (RestClientException e) {
            log.error("Failed to call Bailian API", e);
            throw new RuntimeException("调用 AI 服务失败，请检查 API Key 和网络连接: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 AI 返回的 JSON，提取选题结果
     */
    private AiPaperResult parseAiResponse(String aiContent, List<Question> candidates) {
        // 提取 JSON（可能包裹在 markdown 代码块中）
        String json = extractJson(aiContent);

        Map<String, Object> resultMap;
        try {
            resultMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response JSON: {}", aiContent);
            throw new RuntimeException("AI 返回格式异常，请重试", e);
        }

        // 构建 ID -> Question 映射
        Map<Long, Question> questionMap = candidates.stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

        // 解析选题列表
        List<Map<String, Object>> questions = (List<Map<String, Object>>) resultMap.get("questions");
        if (questions == null || questions.isEmpty()) {
            throw new RuntimeException("AI 未返回任何题目建议，请调整组卷条件后重试");
        }

        AiPaperResult result = new AiPaperResult();
        result.setPaperName((String) resultMap.getOrDefault("paperName", "AI 组卷"));
        result.setSummary((String) resultMap.getOrDefault("summary", ""));
        result.setCandidateCount(candidates.size());

        List<AiPaperResult.SuggestedQuestion> suggested = new ArrayList<>();
        double totalScore = 0;

        for (Map<String, Object> qm : questions) {
            Object idObj = qm.get("questionId");
            Long questionId = idObj instanceof Integer ? ((Integer) idObj).longValue() : (Long) idObj;
            Double score = qm.get("questionScore") instanceof Integer
                    ? ((Integer) qm.get("questionScore")).doubleValue()
                    : (Double) qm.get("questionScore");
            String reason = (String) qm.getOrDefault("reason", "");

            Question question = questionMap.get(questionId);
            if (question == null) {
                log.warn("AI returned unknown questionId: {}", questionId);
                continue;
            }

            if (score == null) {
                score = question.getScore() != null ? question.getScore() : 5.0;
            }

            suggested.add(AiPaperResult.SuggestedQuestion.fromQuestion(question, score, reason));
            totalScore += score;
        }

        result.setSuggestedQuestions(suggested);
        result.setTotalScore(Math.round(totalScore * 100.0) / 100.0);

        return result;
    }

    /**
     * 从 AI 返回内容中提取 JSON（兼容 markdown 代码块包裹）
     */
    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            throw new RuntimeException("AI 返回内容为空");
        }

        // 尝试匹配 ```json ... ``` 代码块
        Pattern pattern = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 尝试匹配 { ... } 最外层 JSON
        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return content.substring(firstBrace, lastBrace + 1).trim();
        }

        return content.trim();
    }

    // ==================== AI 课件总结 ====================
    /**
     * 调用 AI 对课件内容进行摘要总结
     *
     * @param resourceName 资源名称
     * @param fileContent 提取出的文本内容（媒体文件为占位描述）
     * @param courseName 所属课程名称（可选，用于提供上下文）
     * @return AI 生成的总结文本
     */
    public String summarizeResource(String resourceName, String fileContent, String courseName) {
        if (fileContent == null || fileContent.isBlank()) {
            return "课件内容为空，无法生成摘要。";
        }

        // 限制文本长度，避免超出 token 限制（保留约前 8000 字）
        int maxChars = 8000;
        String truncated = fileContent.length() > maxChars
                ? fileContent.substring(0, maxChars) + "\n...(内容已截断)"
                : fileContent;

        // 判断是否为媒体文件（无法提取文字的类型）
        boolean isMedia = truncated.startsWith("[视频文件]")
                || truncated.startsWith("[音频文件]")
                || truncated.startsWith("[图片文件]")
                || truncated.startsWith("[不可读文件]");

        String systemPrompt;
        if (isMedia) {
            systemPrompt = """
                你是一位专业的教育内容分析助手。学生上传了一份无法直接提取文字的媒体课件，
                你需要根据文件名和课程信息，为学生提供有价值的学习指导。

                请按以下格式输出（使用 Markdown 格式）：
                1. **学习目标预估**（根据文件名和课程名，推测本课件可能涉及的知识点，列出 3-5 条）
                2. **内容简介**（一段话，约 100-200 字，说明这是什么类型的课件、可能涵盖的内容范围）
                3. **相关概念**（根据文件名关键词，列出 3-6 个可能涉及的核心概念，附带简短解释）
                4. **观看/学习建议**（3-5 条针对该类型文件的具体学习建议）

                要求：
                - 语言亲切、鼓励，适合学生阅读
                - 根据文件名尽力推测主题，不要只说"无法分析"
                - 学习建议要具体可操作，不要泛泛而谈
                - 在开头简短说明"这是一份{视频/音频/图片}课件，以下分析基于文件名推测，具体内容请观看后确认"
                """;
        } else {
            systemPrompt = """
                你是一位专业的教育内容分析助手。你的任务是对课件内容进行结构化总结，帮助学生快速理解重点。

                请按以下格式输出总结（使用 Markdown 格式）：
                1. **核心要点**（3-5 条，每条一句话）
                2. **内容概述**（一段话，约 150-300 字）
                3. **关键概念**（列出 3-8 个关键词/概念，附带简短解释）
                4. **学习建议**（1-3 条学习该内容的建议）

                要求：
                - 语言简洁明了，适合学生阅读
                - 如果原文是代码/技术文档，请关注核心逻辑和重点
                - 如果原文内容较少或无法理解，请如实说明
                """;
        }

        String contextInfo = "";
        if (courseName != null && !courseName.isBlank()) {
            contextInfo = "（所属课程：" + courseName + "）";
        }

        String userPrompt = "请对以下课件《" + resourceName + "》" + contextInfo + "的内容进行总结：\n\n" + truncated;

        String aiResponse = callChatApiText(systemPrompt, userPrompt);
        return aiResponse != null ? aiResponse.trim() : "AI 服务暂时不可用，请稍后重试。";
    }
}
