package com.nl2sql.common.enums;

import com.nl2sql.common.i18n.MessageUtils;

/**
 * 结果/错误码契约。统一 REST 响应码与业务异常码的来源，
 * 让 {@code R<T>} 与 {@link com.nl2sql.common.exception.BaseException} 共享同一套编码。
 * <p>各服务可定义自己的枚举实现本接口，扩展专属错误码。
 */
public interface IResultCode extends IEnum<Integer> {

    /** 响应/错误码 */
    Integer getCode();

    /** 提示消息（内置默认文案，无 i18n 环境时兜底） */
    @Override
    String getDesc();

    /** i18n 消息 key；默认无 key，返回 null 表示不走国际化 */
    default String getI18nKey() {
        return null;
    }

    /** 按当前 locale 返回文案：有 i18n key 则解析，失败或无 key 回退 {@link #getDesc()} */
    default String getMessage() {
        String key = getI18nKey();
        if (key == null) {
            return getDesc();
        }
        String msg = MessageUtils.get(key);
        return key.equals(msg) ? getDesc() : msg;
    }
}

