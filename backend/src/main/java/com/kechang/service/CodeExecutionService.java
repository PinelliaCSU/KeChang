package com.kechang.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kechang.dto.CodeRunRequest;
import com.kechang.dto.CodeRunResult;
import com.kechang.entity.Question;
import com.kechang.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 代码执行服务 — 通过 Docker 沙箱安全执行用户提交的代码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeExecutionService {

    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    /**
     * 编译超时（秒）
     */
    private static final int COMPILE_TIMEOUT = 15;
    /**
     * 默认执行超时（秒）
     */
    private static final int DEFAULT_EXEC_TIMEOUT = 5;
    /**
     * 总超时（秒）
     */
    private static final int TOTAL_TIMEOUT = 30;

    /**
     * 语言 → Docker 镜像映射
     */
    private static final Map<String, String> IMAGE_MAP = Map.of(
            "c", "kechang/runner-c",
            "cpp", "kechang/runner-cpp",
            "python", "kechang/runner-python"
    );

    /**
     * 语言 → 源文件名映射
     */
    private static final Map<String, String> SOURCE_FILE = Map.of(
            "c", "main.c",
            "cpp", "main.cpp",
            "python", "main.py"
    );

    // ==================== 公共方法 ====================
    /**
     * 运行代码（仅用指定测试用例测试，不记录数据库）
     */
    public CodeRunResult runCode(CodeRunRequest request) {
        List<CodeRunRequest.TestCase> testCases = getTestCases(request);
        return executeAndJudge(request.getLanguage(), request.getSourceCode(), testCases, null);
    }

    /**
     * 根据 questionId 获取题目中的所有测试用例并判题
     */
    public CodeRunResult judgeByQuestion(String language, String sourceCode, Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null || question.getProgrammingConfig() == null) {
            return CodeRunResult.builder()
                    .status("ERROR")
                    .compileError("题目配置不存在")
                    .build();
        }

        ProgrammingConfig config = parseConfig(question.getProgrammingConfig());
        List<CodeRunRequest.TestCase> allCases = new ArrayList<>();

        // 合并可见和隐藏测试用例
        if (config.getTestCases() != null) {
            allCases.addAll(config.getTestCases());
        }
        if (config.getHiddenTestCases() != null) {
            allCases.addAll(config.getHiddenTestCases());
        }

        int timeLimit = config.getTimeLimit() != null ? config.getTimeLimit() : DEFAULT_EXEC_TIMEOUT;
        return executeAndJudge(language, sourceCode, allCases, timeLimit);
    }

    // ==================== 核心执行逻辑 ====================
    private CodeRunResult executeAndJudge(String language, String sourceCode,
            List<CodeRunRequest.TestCase> testCases,
            Integer timeLimitSec) {
        if (!IMAGE_MAP.containsKey(language)) {
            return CodeRunResult.builder()
                    .status("ERROR")
                    .compileError("不支持的编程语言: " + language)
                    .build();
        }

        String image = IMAGE_MAP.get(language);
        String fileName = SOURCE_FILE.get(language);
        int execTimeout = timeLimitSec != null ? timeLimitSec : DEFAULT_EXEC_TIMEOUT;
        String workDir = createTempWorkDir();

        try {
            // 写入源代码
            Path sourcePath = Path.of(workDir, fileName);
            Files.writeString(sourcePath, sourceCode, StandardCharsets.UTF_8);

            // 编译（C / C++）
            String compileError = null;
            if ("c".equals(language) || "cpp".equals(language)) {
                compileError = compileCode(workDir, image, fileName, language);
                if (compileError != null) {
                    return buildCEResult(compileError, testCases.size());
                }
            }

            // 逐个用例执行
            List<CodeRunResult.CaseResult> caseResults = new ArrayList<>();
            int passed = 0;
            int maxTime = 0;
            int maxMemory = 0;

            for (int i = 0; i < testCases.size(); i++) {
                CodeRunRequest.TestCase tc = testCases.get(i);
                CodeRunResult.CaseResult cr = executeSingleCase(
                        workDir, image, language, tc, execTimeout, i);
                caseResults.add(cr);

                if (cr.isPassed()) {
                    passed++;
                }
                if (cr.getTime() != null && cr.getTime() > maxTime) {
                    maxTime = cr.getTime();
                }
                if (cr.getMemory() != null && cr.getMemory() > maxMemory) {
                    maxMemory = cr.getMemory();
                }
            }

            String status = (passed == testCases.size() && testCases.size() > 0) ? "AC" : "WA";
            return CodeRunResult.builder()
                    .status(status)
                    .passedCases(passed)
                    .totalCases(testCases.size())
                    .maxTime(maxTime)
                    .maxMemory(maxMemory)
                    .caseResults(caseResults)
                    .build();

        } catch (IOException e) {
            log.error("代码执行IO异常", e);
            return CodeRunResult.builder()
                    .status("ERROR")
                    .compileError("系统错误: " + e.getMessage())
                    .build();
        } finally {
            cleanupWorkDir(workDir);
        }
    }

    // ==================== 编译 ====================
    private String compileCode(String workDir, String image, String fileName, String language) {
        String compiler = "c".equals(language) ? "gcc" : "g++";
        String compileCmd = String.format(
                "%s /code/%s -o /code/program -O2 -Wall -lm 2>/code/compile_error.txt",
                compiler, fileName);

        int exitCode = runDocker(workDir, image, "sh", "-c", compileCmd, String.valueOf(COMPILE_TIMEOUT));

        try {
            Path errorFile = Path.of(workDir, "compile_error.txt");
            if (Files.exists(errorFile)) {
                String error = Files.readString(errorFile, StandardCharsets.UTF_8).trim();
                if (!error.isEmpty()) {
                    return error;
                }
            }
        } catch (IOException ignored) {
        }

        // 检查编译产物是否存在
        Path program = Path.of(workDir, "program");
        if (exitCode != 0 || !Files.exists(program)) {
            return "编译失败，退出码: " + exitCode;
        }

        return null; // 编译成功
    }

    // ==================== 单用例执行 ====================
    private CodeRunResult.CaseResult executeSingleCase(
            String workDir, String image, String language,
            CodeRunRequest.TestCase tc, int execTimeout, int caseIndex) {

        long startTime = System.currentTimeMillis();

        try {
            // 写入输入文件
            Path inputFile = Path.of(workDir, "input.txt");
            String input = tc.getInput() != null ? tc.getInput() : "";
            Files.writeString(inputFile, input, StandardCharsets.UTF_8);

            // 构造执行命令
            String execCmd;
            if ("python".equals(language)) {
                execCmd = String.format("timeout %d python3 /code/main.py < /code/input.txt > /code/output.txt 2>/code/error.txt", execTimeout);
            } else {
                execCmd = String.format("timeout %d /code/program < /code/input.txt > /code/output.txt 2>/code/error.txt", execTimeout);
            }

            int exitCode = runDocker(workDir, image, "sh", "-c", execCmd, String.valueOf(execTimeout + 5));

            long elapsed = System.currentTimeMillis() - startTime;

            // 检查是否超时（timeout 命令返回 124 表示超时）
            if (exitCode == 124) {
                return CodeRunResult.CaseResult.builder()
                        .passed(false).status("TLE")
                        .input(tc.getInput()).expectedOutput(tc.getExpectedOutput())
                        .error("运行超时 (" + execTimeout + "s)")
                        .time((int) elapsed).build();
            }

            // 检查运行时错误
            Path errorFile = Path.of(workDir, "error.txt");
            String errorOutput = "";
            if (Files.exists(errorFile)) {
                errorOutput = Files.readString(errorFile, StandardCharsets.UTF_8).trim();
            }

            if (exitCode != 0 && !errorOutput.isEmpty()) {
                return CodeRunResult.CaseResult.builder()
                        .passed(false).status("RE")
                        .input(tc.getInput()).expectedOutput(tc.getExpectedOutput())
                        .error(errorOutput).time((int) elapsed).build();
            }

            // 读取实际输出
            Path outputFile = Path.of(workDir, "output.txt");
            String actualOutput = "";
            if (Files.exists(outputFile)) {
                actualOutput = Files.readString(outputFile, StandardCharsets.UTF_8);
            }

            // 对比输出
            String expected = tc.getExpectedOutput() != null ? tc.getExpectedOutput() : "";
            boolean passed = normalizeOutput(actualOutput).equals(normalizeOutput(expected));

            return CodeRunResult.CaseResult.builder()
                    .passed(passed).status(passed ? "AC" : "WA")
                    .input(tc.getInput()).expectedOutput(tc.getExpectedOutput())
                    .actualOutput(actualOutput).error(errorOutput.isEmpty() ? null : errorOutput)
                    .time((int) elapsed).build();

        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return CodeRunResult.CaseResult.builder()
                    .passed(false).status("RE")
                    .input(tc.getInput()).expectedOutput(tc.getExpectedOutput())
                    .error("IO异常: " + e.getMessage()).time((int) elapsed).build();
        }
    }

    // ==================== Docker 调用 ====================
    /**
     * 执行 Docker 容器命令
     */
    private int runDocker(String workDir, String image, String... cmdArgs) {
        List<String> command = new ArrayList<>(List.of(
                "docker", "run", "--rm",
                "--network=none",
                "--memory=256m",
                "--memory-swap=256m",
                "--cpus=1",
                "--security-opt=no-new-privileges",
                "--pids-limit=100",
                "-u", "1000:1000",
                "-v", workDir + ":/code",
                "-w", "/code",
                image
        ));
        command.addAll(Arrays.asList(cmdArgs));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            // 消费 stdout（避免进程阻塞）
            new Thread(() -> {
                try (InputStream is = process.getInputStream()) {
                    is.transferTo(OutputStream.nullOutputStream());
                } catch (IOException ignored) {
                }
            }).start();

            boolean finished = process.waitFor(TOTAL_TIMEOUT, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return -1;
            }
            return process.exitValue();
        } catch (IOException | InterruptedException e) {
            log.error("Docker 执行异常: {}", e.getMessage());
            return -1;
        }
    }

    // ==================== 输出标准化 ====================
    /**
     * 标准化输出用于对比：trim 首尾空白，统一换行符，移除末尾多余空行
     */
    private String normalizeOutput(String output) {
        if (output == null) {
            return "";
        }
        return output.trim()
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("\n+$", "");
    }

    // ==================== 辅助方法 ====================
    private String createTempWorkDir() {
        try {
            Path dir = Files.createTempDirectory("kechang-code-");
            return dir.toString();
        } catch (IOException e) {
            throw new RuntimeException("无法创建临时工作目录", e);
        }
    }

    private void cleanupWorkDir(String workDir) {
        try {
            Path dir = Path.of(workDir);
            if (Files.exists(dir)) {
                try (var stream = Files.walk(dir)) {
                    stream.sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException ignored) {
                                }
                            });
                }
            }
        } catch (IOException ignored) {
        }
    }

    private CodeRunResult buildCEResult(String compileError, int totalCases) {
        return CodeRunResult.builder()
                .status("CE")
                .passedCases(0)
                .totalCases(totalCases)
                .compileError(compileError)
                .caseResults(Collections.emptyList())
                .build();
    }

    private List<CodeRunRequest.TestCase> getTestCases(CodeRunRequest request) {
        if (request.getTestCases() != null && !request.getTestCases().isEmpty()) {
            return request.getTestCases();
        }
        if (request.getQuestionId() != null) {
            Question question = questionMapper.selectById(request.getQuestionId());
            if (question != null && question.getProgrammingConfig() != null) {
                ProgrammingConfig config = parseConfig(question.getProgrammingConfig());
                if (config.getTestCases() != null) {
                    return config.getTestCases();
                }
            }
        }
        return Collections.emptyList();
    }

    private ProgrammingConfig parseConfig(String json) {
        try {
            return objectMapper.readValue(json, ProgrammingConfig.class);
        } catch (JsonProcessingException e) {
            log.error("解析编程题配置失败", e);
            return new ProgrammingConfig();
        }
    }

    /**
     * 编程题配置内部类（映射 programming_config JSON 字段）
     */
    @lombok.Data
    public static class ProgrammingConfig {

        private String language;                              // c / cpp / python
        private String templateCode;                          // 模板/起始代码
        private Integer timeLimit = DEFAULT_EXEC_TIMEOUT;     // 时间限制(秒)
        private Integer memoryLimit = 256;                    // 内存限制(MB)
        private String scoringMode = "perCase";               // perCase / allOrNothing
        private List<CodeRunRequest.TestCase> testCases;       // 可见测试用例
        private List<CodeRunRequest.TestCase> hiddenTestCases; // 隐藏测试用例
    }
}
