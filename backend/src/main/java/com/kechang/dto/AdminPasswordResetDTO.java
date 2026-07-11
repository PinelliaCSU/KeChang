package com.kechang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置用户密码请求 DTO
 */
@Data
public class AdminPasswordResetDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "密码长度至少6位")
    private String newPassword;
}
