package com.kechang.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员创建用户请求 DTO
 */
@Data
public class AdminUserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6位")
    private String password;

    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    @NotNull(message = "请选择角色")
    private Long roleId;

    // ---- 教师扩展字段 ----
    private String realName;
    private String title;
    private String department;

    // ---- 学生扩展字段 ----
    private String studentNo;
    private String grade;
    private String major;
    private String className;
}
