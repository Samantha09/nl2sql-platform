package com.nl2sql.common.i18n;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * 国际化自动装配：构建 {@link MessageSource} 并注入 {@link MessageUtils}，供全局静态调用。
 * <p>资源文件位于 classpath 的 {@code i18n/messages*.properties}，可被各服务自定义同名文件扩展。
 * web 场景的「按 Accept-Language 切换 locale」由 {@link I18nWebConfiguration} 单独装配，
 * 以免在非 web 模块引入 servlet 依赖。
 */
@AutoConfiguration
@EnableConfigurationProperties(I18nProperties.class)
public class I18nAutoConfiguration {

    @Bean("nl2sqlMessageSource")
    public MessageSource nl2sqlMessageSource(I18nProperties props) {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames(props.getBasenames().toArray(new String[0]));
        source.setDefaultEncoding(props.getEncoding());
        // 找不到 key 时回退为 code 本身，避免 NoSuchMessageException
        source.setUseCodeAsDefaultMessage(true);
        source.setCacheSeconds(props.getCacheSeconds());
        // 注入静态工具，使 MessageUtils.get(...) 全局可用
        MessageUtils.init(source);
        return source;
    }
}
