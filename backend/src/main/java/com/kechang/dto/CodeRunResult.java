package com.kechang.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 代码运行结果 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeRunResult {

    /**
     * 整体判题状态：AC / WA / TLE / MLE / RE / CE
     */
    private String status;

    /**
     * 通过用例数
     */
    private int passedCases;

    /**
     * 总用例数
     */
    private int totalCases;

    /**
     * 编译错误信息（CE 时填充）
     */
    private String compileError;

    /**
     * 最大执行时间(ms)
     */
    private Integer maxTime;

    /**
     * 最大内存使用(KB)
     */
    private Integer maxMemory;

    /**
     * 每个用例的详细结果
     */
    private List<CaseResult> caseResults;

    /**
     * 单个用例的运行结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaseResult {

        /**
         * 是否通过
         */
        private boolean passed;

        /**
         * 状态：AC / WA / TLE / MLE / RE
         */
        private String status;

        /**
         * 输入
         */
        private String input;

        /**
         * 预期输出
         */
        private String expectedOutput;

        /**
         * 实际输出
         */
        private String actualOutput;

        /**
         * 错误信息（RE 时捕获的 stderr 或异常）
         */
        private String error;

        /**
         * 执行时间(ms)
         */
        private Integer time;

        /**
         * 内存使用(KB)
         */
        private Integer memory;
    }
}
