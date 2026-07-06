package com.kechang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码 DTO
 */
@Data
public class PasswordDTO {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
