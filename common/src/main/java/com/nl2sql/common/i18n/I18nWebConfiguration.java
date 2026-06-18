package com.nl2sql.common.i18n;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;

/**
 * web 场景的 locale 解析：依请求头 {@code Accept-Language} 决定当前 locale，
 * 写入 {@code LocaleContextHolder}，供 {@link MessageUtils} 读取。
 * <p>仅在 servlet web 应用且引入 spring-webmvc 时装配，非 web 模块无副作用。
 */
@AutoConfiguration(after = I18nAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(LocaleResolver.class)
public class I18nWebConfiguration {

    @Bean
    @ConditionalOnMissingBean(LocaleResolver.class)
    public LocaleResolver localeResolver(I18nProperties props) {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(props.getDefaultLocale());
        resolver.setSupportedLocales(List.of(
                java.util.Locale.SIMPLIFIED_CHINESE,
                java.util.Locale.ENGLISH));
        return resolver;
    }
}
