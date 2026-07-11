package com.kechang.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员端用户列表 VO
 */
@Data
public class AdminUserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private Integer status;
    private LocalDateTime createTime;

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
