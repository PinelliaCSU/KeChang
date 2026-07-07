package com.kechang.dto;

import lombok.Data;

/**
 * 更新用户资料 DTO
 */
@Data
public class ProfileDTO {

    private String nickname;
    private String phone;
    private String email;
    private String avatar;
}
