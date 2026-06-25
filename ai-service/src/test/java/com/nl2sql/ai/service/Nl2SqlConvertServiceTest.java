package com.nl2sql.ai.service;

import com.nl2sql.ai.config.LlmProperties;
import com.nl2sql.ai.llm.IntentDetector;
import com.nl2sql.ai.llm.LlmClient;
import com.nl2sql.ai.llm.SchemaContextBuilder;
import com.nl2sql.common.dto.ConvertRequest;
import com.nl2sql.common.dto.ConvertResponse;
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
    @Mock private IntentDetector intentDetector;
    private final LlmProperties properties = new LlmProperties();
    private Nl2SqlConvertService service;

    @BeforeEach
    void setUp() {
        properties.setFallbackToMock(false);
        service = new Nl2SqlConvertService(llmClient, schemaContextBuilder, mockLLMService, properties, intentDetector);
    }

    @Test
    @DisplayName("convert 对 query 意图应拼上下文并调用 LLM 返回 SQL")
    void shouldCallLlmWithContext() {
        ConvertRequest req = new ConvertRequest();
        req.setDataSourceId(1L);
        req.setNaturalLanguage("查销售额");
        req.setDatabaseName("shop");
        when(intentDetector.detect("查销售额")).thenReturn(new IntentDetector.Result(IntentDetector.Intent.QUERY, null));
        when(schemaContextBuilder.build(1L, "shop")).thenReturn("ctx");
        when(llmClient.chat("ctx", "查销售额")).thenReturn("SELECT * FROM orders;");

        ConvertResponse resp = service.convert(req);

        assertThat(resp.getType()).isEqualTo("sql");
        assertThat(resp.getContent()).isEqualTo("SELECT * FROM orders;");
    }

    @Test
    @DisplayName("convert 对 chat 意图应直接返回文本回复，不调 SQL 生成")
    void shouldReturnChatReplyForGreeting() {
        ConvertRequest req = new ConvertRequest();
        req.setDataSourceId(1L);
        req.setNaturalLanguage("你好");
        req.setDatabaseName("shop");
        when(intentDetector.detect("你好")).thenReturn(
                new IntentDetector.Result(IntentDetector.Intent.CHAT, "你好！有什么可以帮你的吗？"));

        ConvertResponse resp = service.convert(req);

        assertThat(resp.getType()).isEqualTo("chat");
        assertThat(resp.getContent()).isEqualTo("你好！有什么可以帮你的吗？");
    }

    @Test
    @DisplayName("convert 应清理 LLM 返回的 markdown 代码块")
    void shouldStripMarkdownCodeBlock() {
        ConvertRequest req = new ConvertRequest();
        req.setDataSourceId(1L);
        req.setNaturalLanguage("查销售额");
        req.setDatabaseName("shop");
        when(intentDetector.detect("查销售额")).thenReturn(new IntentDetector.Result(IntentDetector.Intent.QUERY, null));
        when(schemaContextBuilder.build(1L, "shop")).thenReturn("ctx");
        when(llmClient.chat("ctx", "查销售额"))
                .thenReturn("```sql\nSELECT * FROM orders;\n```");

        ConvertResponse resp = service.convert(req);

        assertThat(resp.getType()).isEqualTo("sql");
        assertThat(resp.getContent()).isEqualTo("SELECT * FROM orders;");
    }

    @Test
    @DisplayName("LLM 异常且 fallbackToMock=true 时降级到 MockLLMService")
    void shouldFallbackToMockWhenEnabled() {
        properties.setFallbackToMock(true);
        ConvertRequest req = new ConvertRequest();
        req.setDataSourceId(1L);
        req.setNaturalLanguage("销售额");
        req.setDatabaseName("shop");
        when(intentDetector.detect("销售额")).thenReturn(new IntentDetector.Result(IntentDetector.Intent.QUERY, null));
        when(schemaContextBuilder.build(1L, "shop")).thenReturn("ctx");
        when(llmClient.chat("ctx", "销售额")).thenThrow(new IllegalStateException("boom"));
        when(mockLLMService.convert("销售额", 1L)).thenReturn("SELECT 1;");

        ConvertResponse resp = service.convert(req);

        assertThat(resp.getType()).isEqualTo("sql");
        assertThat(resp.getContent()).isEqualTo("SELECT 1;");
    }
}
