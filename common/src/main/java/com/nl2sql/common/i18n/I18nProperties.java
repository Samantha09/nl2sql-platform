package com.nl2sql.common.i18n;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Locale;

/**
 * 国际化可配置项，前缀 {@code nl2sql.i18n}。
 *
 * <pre>
 * nl2sql:
 *   i18n:
 *     basenames:
 *       - i18n/messages
 *     encoding: UTF-8
 *     default-locale: zh_CN
 *     cache-seconds: -1      # -1 永久缓存；开发期可设 0 实时刷新
 * </pre>
 */
@ConfigurationProperties(prefix = "nl2sql.i18n")
public class I18nProperties {

    /** 资源文件 basename（classpath 相对路径，不含语言后缀） */
    private List<String> basenames = List.of("i18n/messages");

    /** 资源文件编码 */
    private String encoding = "UTF-8";

    /** 默认 locale（无 Accept-Language 时使用） */
    private Locale defaultLocale = Locale.SIMPLIFIED_CHINESE;

    /** 资源缓存秒数，-1 表示永久缓存 */
    private int cacheSeconds = -1;

    public List<String> getBasenames() {
        return basenames;
    }

    public void setBasenames(List<String> basenames) {
        this.basenames = basenames;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public int getCacheSeconds() {
        return cacheSeconds;
    }

    public void setCacheSeconds(int cacheSeconds) {
        this.cacheSeconds = cacheSeconds;
    }
}
