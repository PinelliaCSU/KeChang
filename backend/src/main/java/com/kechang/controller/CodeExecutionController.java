package com.kechang.controller;

import com.kechang.common.Result;
import com.kechang.dto.CodeRunRequest;
import com.kechang.dto.CodeRunResult;
import com.kechang.service.CodeExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 代码执行控制器 — 处理在线编程代码运行和判题
 */
@RestController
@RequiredArgsConstructor
public class CodeExecutionController {

    private final CodeExecutionService codeExecutionService;

    /**
     * 运行代码（学生考试中测试样例） 仅运行可见的测试用例，不记录到数据库
     */
    @PostMapping("/api/code/run")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_TEACHER')")
    public Result<CodeRunResult> runCode(@RequestBody CodeRunRequest request) {
        CodeRunResult result = codeExecutionService.runCode(request);
        return Result.success(result);
    }

    /**
     * 提交判题（运行全部测试用例含隐藏用例） 用于考试交卷时的自动判分，结果会记录到数据库
     */
    @PostMapping("/api/code/judge")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_TEACHER')")
    public Result<CodeRunResult> judgeCode(@RequestBody CodeRunRequest request) {
        if (request.getQuestionId() == null) {
            return Result.error(400, "判题需要指定题目ID");
        }
        CodeRunResult result = codeExecutionService.judgeByQuestion(
                request.getLanguage(), request.getSourceCode(), request.getQuestionId());
        return Result.success(result);
    }
}
