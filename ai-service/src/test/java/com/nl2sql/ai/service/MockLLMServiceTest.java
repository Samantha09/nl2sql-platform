package com.nl2sql.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockLLMService - Mock LLM 转换逻辑")
class MockLLMServiceTest {

    private final MockLLMService service = new MockLLMService();

    @ParameterizedTest(name = "[{0}] 应转换为 [{1}]")
    @CsvSource({
            "本月销售, 'SELECT product_name, SUM(amount) AS total_sales FROM orders GROUP BY product_name ORDER BY total_sales DESC LIMIT 10;'",
            "sales report, 'SELECT product_name, SUM(amount) AS total_sales FROM orders GROUP BY product_name ORDER BY total_sales DESC LIMIT 10;'",
            "用户总数, 'SELECT COUNT(*) AS user_count FROM users;'",
            "user count, 'SELECT COUNT(*) AS user_count FROM users;'",
            "随便查一下, 'SELECT * FROM orders LIMIT 100;'"
    })
    @DisplayName("convert 应根据关键词返回对应 SQL")
    void shouldConvertByKeyword(String naturalLanguage, String expectedSql) {
        String sql = service.convert(naturalLanguage, 1L);
        assertThat(sql).isEqualTo(expectedSql);
    }
}
