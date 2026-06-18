package com.nl2sql.common.enums;

/**
 * 平台通用错误码。各服务可复用这些通用码，专属业务码另建枚举实现 {@link IResultCode}。
 * <p>编码沿用 HTTP 语义区间，便于网关与前端统一处理。
 * <p>每个枚举绑定一个 i18n key，{@link #getMessage()} 按当前 locale 返回国际化文案，
 * {@link #getDesc()} 返回内置中文默认值（无 i18n 环境时兜底）。
 */
public enum ResultCode implements IResultCode {

    SUCCESS(200, "result.success", "操作成功"),
    BAD_REQUEST(400, "result.bad_request", "请求参数错误"),
    UNAUTHORIZED(401, "result.unauthorized", "未认证"),
    FORBIDDEN(403, "result.forbidden", "无权限"),
    NOT_FOUND(404, "result.not_found", "资源不存在"),
    INTERNAL_ERROR(500, "result.internal_error", "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "result.service_unavailable", "服务不可用");

    private final Integer code;
    private final String i18nKey;
    private final String desc;

    ResultCode(Integer code, String i18nKey, String desc) {
        this.code = code;
        this.i18nKey = i18nKey;
        this.desc = desc;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    /** 内置默认描述（中文），无 i18n 环境时的兜底文案 */
    @Override
    public String getDesc() {
        return desc;
    }

    /** i18n 消息 key */
    @Override
    public String getI18nKey() {
        return i18nKey;
    }
}

