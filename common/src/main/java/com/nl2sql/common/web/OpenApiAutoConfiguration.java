package com.nl2sql.common.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * OpenAPI 文档自动配置：为每个 web 服务生成统一标题的 Swagger 文档。
 * <p>仅当 classpath 存在 springdoc（{@link OpenAPI}）时装配。</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OpenAPI.class)
public class OpenApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI nl2sqlOpenAPI(@Value("${spring.application.name:nl2sql-service}") String appName) {
        return new OpenAPI().info(new Info()
                .title("NL2SQL 平台 - " + appName + " API")
                .description("自然语言查询数据库平台接口文档")
                .version("0.0.1"));
    }
}
