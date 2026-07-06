package com.kechang.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结果类
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
public class Result<T> {

    /**
     * 状态码
     */
    private int code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 数据体
     */
    private T data;

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ---- 成功 ----
    @SuppressWarnings("unchecked")
    public static <T> Result<T> success() {
        return new Result(200, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    // ---- 失败 ----
    @SuppressWarnings("unchecked")
    public static <T> Result<T> error(int code, String message) {
        return new Result(code, message, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> Result<T> error(String message) {
        return new Result(500, message, null);
    }

    // ---- 常用错误码 ----
    @SuppressWarnings("unchecked")
    public static <T> Result<T> unauthorized(String message) {
        return new Result(401, message, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> Result<T> forbidden(String message) {
        return new Result(403, message, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> Result<T> notFound(String message) {
        return new Result(404, message, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> Result<T> badRequest(String message) {
        return new Result(400, message, null);
    }
}
