package com.kechang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 代码运行请求 DTO
 */
@Data
public class CodeRunRequest {

    /**
     * 编程语言：c / cpp / python
     */
    @NotBlank(message = "编程语言不能为空")
    private String language;

    /**
     * 源代码
     */
    @NotBlank(message = "代码不能为空")
    @Size(max = 50000, message = "代码长度不能超过50000字符")
    private String sourceCode;

    /**
     * 题目ID（可选，用于查找测试用例）
     */
    private Long questionId;

    /**
     * 自定义测试用例（如果传了 questionId，可覆盖题库中的用例）
     */
    private List<TestCase> testCases;

    /**
     * 单个测试用例
     */
    @Data
    public static class TestCase {

        /**
         * 输入（stdin）
         */
        private String input;
        /**
         * 预期输出
         */
        private String expectedOutput;
    }
}
