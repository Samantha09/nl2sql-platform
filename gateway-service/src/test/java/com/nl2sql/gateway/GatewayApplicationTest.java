package com.nl2sql.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("GatewayApplication - 网关上下文加载")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class GatewayApplicationTest {

    @Test
    @DisplayName("应能正常加载 Spring 上下文")
    void contextLoads() {
        // 只要上下文启动成功即通过
    }
}
