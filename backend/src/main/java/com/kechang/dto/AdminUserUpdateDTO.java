package com.kechang.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * 管理员更新用户请求 DTO（所有字段可选，部分更新）
 */
@Data
public class AdminUserUpdateDTO {

    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

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
