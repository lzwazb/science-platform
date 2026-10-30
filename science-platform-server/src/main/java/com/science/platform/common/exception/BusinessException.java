package com.science.platform.common.exception;

/**
 * 业务异常：由 Service 层抛出，携带业务状态码，由全局异常处理器转换成统一响应。
 *
 * <p>约定：状态码与 HTTP 语义保持一致（400 参数/业务规则、403 无权限、404 不存在）。</p>
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }
}
