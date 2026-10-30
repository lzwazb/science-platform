package com.science.platform.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 统一响应结构（对应开发档案 8.1 节）。
 *
 * <pre>
 * { "code": 200, "message": "success", "data": {}, "timestamp": "2026-10-29T12:00:00" }
 * </pre>
 *
 * <p>约定：code 与 HTTP 状态码保持一致，success 使用 200，业务错误沿用 400/401/403/404/500。</p>
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {

    /** 业务码，与 HTTP 状态码语义一致 */
    private int code;

    /** 提示信息，成功为 success，失败为用户可读的中文说明 */
    private String message;

    /** 业务数据，失败时为 null */
    private T data;

    /** 服务端响应时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(int code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
