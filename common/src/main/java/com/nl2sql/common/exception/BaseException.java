package com.nl2sql.common.exception;

import com.nl2sql.common.enums.IResultCode;
import com.nl2sql.common.enums.ResultCode;

/**
 * 业务异常父类。携带 {@link IResultCode}，便于全局异常处理器据此构造统一响应。
 * <p>各服务可直接抛出本类，或继承它定义领域专属异常。运行时异常，不强制 try-catch。
 */
public class BaseException extends RuntimeException {

    /** 错误码 */
    private final Integer code;

    /** 以结果码构造，消息取自结果码描述 */
    public BaseException(IResultCode resultCode) {
        super(resultCode.getDesc());
        this.code = resultCode.getCode();
    }

    /** 以结果码 + 自定义消息构造（覆盖默认描述） */
    public BaseException(IResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /** 以自定义码 + 消息构造 */
    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /** 默认 500，仅自定义消息 */
    public BaseException(String message) {
        super(message);
        this.code = ResultCode.INTERNAL_ERROR.getCode();
    }

    /** 携带根因 */
    public BaseException(IResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.code = resultCode.getCode();
    }

    public Integer getCode() {
        return code;
    }
}
