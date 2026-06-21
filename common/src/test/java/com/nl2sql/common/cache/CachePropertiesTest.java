package com.nl2sql.common.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheProperties - 缓存配置属性绑定")
@SpringBootTest(classes = com.nl2sql.common.TestCommonApplication.class)
@EnableConfigurationProperties(CacheProperties.class)
@ActiveProfiles("test")
class CachePropertiesTest {

    @Autowired
    private CacheProperties properties;

    @Test
    @DisplayName("应绑定自定义 keyPrefix、defaultTtl、nullTtl、cacheNullValues")
    void shouldBindCustomValues() {
        assertThat(properties.getKeyPrefix()).isEqualTo("test:");
        assertThat(properties.getDefaultTtl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getNullTtl()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.isCacheNullValues()).isFalse();
    }

    @Test
    @DisplayName("应绑定按 cacheName 的 TTL 覆盖")
    void shouldBindPerCacheTtls() {
        assertThat(properties.getTtls().values())
                .contains(Duration.ofMinutes(30), Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("multiLevel 默认应关闭")
    void shouldDisableMultiLevelByDefault() {
        assertThat(properties.getMultiLevel().isEnabled()).isFalse();
        assertThat(properties.getMultiLevel().getLocalTtls()).isEmpty();
    }
}
