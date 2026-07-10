package com.kechang.controller.admin;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.entity.OperationLog;
import com.kechang.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/operation-logs")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<OperationLog>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action) {
        return Result.success(operationLogService.list(page, size, module, action));
    }

    @GetMapping("/overview")
    public Result<Map<String, Long>> overview() {
        return Result.success(operationLogService.overview());
    }
}
