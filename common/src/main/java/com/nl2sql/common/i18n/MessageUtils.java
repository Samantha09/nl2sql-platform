package com.nl2sql.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * 国际化消息工具。按「当前请求 locale」解析消息文案，供枚举、异常、统一响应使用。
 * <p>locale 来源于 {@link LocaleContextHolder}（web 场景由 LocaleResolver 依 Accept-Language 设置）。
 * 由 {@link I18nAutoConfiguration} 注入 {@link MessageSource} 后即可静态调用。
 */
public final class MessageUtils {

    private static MessageSource messageSource;

    private MessageUtils() {
    }

    static void init(MessageSource source) {
        MessageUtils.messageSource = source;
    }

    /**
     * 解析消息（使用当前 locale）。
     *
     * @param code 消息 key
     * @param args 占位符参数
     * @return 解析后的文案；未配置 MessageSource 或 key 为空时原样返回 code
     */
    public static String get(String code, Object... args) {
        return get(code, LocaleContextHolder.getLocale(), args);
    }

    /** 解析消息（指定 locale） */
    public static String get(String code, Locale locale, Object... args) {
        if (code == null || messageSource == null) {
            return code;
        }
        // useCodeAsDefaultMessage=true 时找不到 key 会回退为 code 本身，不抛异常
        return messageSource.getMessage(code, args, code, locale);
    }
}
