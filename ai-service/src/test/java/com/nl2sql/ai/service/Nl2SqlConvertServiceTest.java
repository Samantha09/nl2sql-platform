package com.nl2sql.ai.service;

import com.nl2sql.ai.config.LlmProperties;
import com.nl2sql.ai.llm.LlmClient;
import com.nl2sql.ai.llm.SchemaContextBuilder;
import com.nl2sql.common.dto.ConvertRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("Nl2SqlConvertService - NL→SQL 整合")
@ExtendWith(MockitoExtension.class)
class Nl2SqlConvertServiceTest {

    @Mock private LlmClient llmClient;
    @Mock private SchemaContextBuilder schemaContextBuilder;
    @Mock private MockLLMService mockLLMService;
    private final LlmProperties properties = new LlmProperties();
    private Nl2SqlConvertService service;

    @BeforeEach
    void setUp() {
        properties.setFallbackToMock(false);
        service = new Nl2SqlConvertService(llmClient, schemaContextBuilder, mockLLMService, properties);
    }

    @Test
    @DisplayName("convert 应拼上下文并调用 LLM 返回 SQL")
    void shouldCallLlmWithContext() {
        ConvertRequest req = new ConvertRequest();
        req.setDataSourceId(1L);
        req.setNaturalLanguage("查销售额");
        req.setDatabaseName("shop");
        when(schemaContextBuilder.build(1L, "shop")).thenReturn("ctx");
        when(llmClient.chat("ctx", "查销售额")).thenReturn("SELECT * FROM orders;");

        assertThat(service.convert(req)).isEqualTo("SELECT * FROM orders;");
    }

    @Test
    @DisplayName("convert 应清理 LLM 返回的 markdown 代码块")
    void shouldStripMarkdownCodeBlock() {
        ConvertRequest req = new ConvertRequest();
        req.setDataSourceId(1L);
        req.setNaturalLanguage("查销售额");
        req.setDatabaseName("shop");
        when(schemaContextBuilder.build(1L, "shop")).thenReturn("ctx");
        when(llmClient.chat("ctx", "查销售额"))
                .thenReturn("```sql\nSELECT * FROM orders;\n```");

        assertThat(service.convert(req)).isEqualTo("SELECT * FROM orders;");
    }

    @Test
    @DisplayName("LLM 异常且 fallbackToMock=true 时降级到 MockLLMService")
    void shouldFallbackToMockWhenEnabled() {
        properties.setFallbackToMock(true);
        ConvertRequest req = new ConvertRequest();
        req.setDataSourceId(1L);
        req.setNaturalLanguage("销售额");
        req.setDatabaseName("shop");
        when(schemaContextBuilder.build(1L, "shop")).thenReturn("ctx");
        when(llmClient.chat("ctx", "销售额")).thenThrow(new IllegalStateException("boom"));
        when(mockLLMService.convert("销售额", 1L)).thenReturn("SELECT 1;");

        assertThat(service.convert(req)).isEqualTo("SELECT 1;");
    }
}
