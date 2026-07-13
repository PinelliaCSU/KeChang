package com.kechang.controller.admin;

import com.kechang.common.PageResult;
import com.kechang.common.Result;
import com.kechang.dto.AdminPasswordResetDTO;
import com.kechang.dto.AdminUserCreateDTO;
import com.kechang.dto.AdminUserUpdateDTO;
import com.kechang.service.AdminUserService;
import com.kechang.vo.AdminUserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员用户管理控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping
    public Result<PageResult<AdminUserVO>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Integer status) {
        size = PageResult.clampSize(size);
        return Result.success(adminUserService.list(page, size, keyword, roleId, status));
    }

    /**
     * 用户统计概览
     */
    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        return Result.success(adminUserService.stats());
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public Result<AdminUserVO> detail(@PathVariable Long id) {
        return Result.success(adminUserService.detail(id));
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody AdminUserCreateDTO dto) {
        adminUserService.create(dto);
        return Result.success();
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateDTO dto) {
        adminUserService.update(id, dto);
        return Result.success();
    }

    /**
     * 切换用户状态（启用/禁用）
     */
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        adminUserService.toggleStatus(id);
        return Result.success();
    }

    /**
     * 重置用户密码
     */
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminPasswordResetDTO dto) {
        adminUserService.resetPassword(id, dto);
        return Result.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminUserService.deleteUser(id, uid());
        return Result.success();
    }
}
