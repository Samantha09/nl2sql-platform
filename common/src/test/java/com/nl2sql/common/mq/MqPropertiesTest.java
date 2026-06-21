package com.nl2sql.common.mq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MqProperties - MQ 配置属性绑定")
@SpringBootTest(classes = com.nl2sql.common.TestCommonApplication.class)
@EnableConfigurationProperties(MqProperties.class)
@ActiveProfiles("test")
class MqPropertiesTest {

    @Autowired
    private MqProperties properties;

    @Test
    @DisplayName("应绑定自定义重试参数")
    void shouldBindCustomValues() {
        assertThat(properties.getMaxRetries()).isEqualTo(5);
        assertThat(properties.getInitialInterval()).isEqualTo(500L);
        assertThat(properties.getMultiplier()).isEqualTo(1.5);
        assertThat(properties.getMaxInterval()).isEqualTo(5000L);
    }
}
