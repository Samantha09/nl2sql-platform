package com.nl2sql.ai.llm;

import com.nl2sql.ai.config.LlmProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("AnthropicMessagesLlmClient - 请求构造与响应解析")
class AnthropicMessagesLlmClientTest {

    private AnthropicMessagesLlmClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        LlmProperties props = new LlmProperties();
        props.setBaseUrl("https://api.minimaxi.com/anthropic");
        props.setApiKey("sk-test-key");
        props.setModel("MiniMax-M2.7");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AnthropicMessagesLlmClient(builder.baseUrl(props.getBaseUrl()).build(), props);
    }

    @Test
    @DisplayName("chat 应携带 x-api-key/anthropic-version 头并解析 content[0].text")
    void shouldCallEndpointAndParseText() {
        server.expect(requestTo("https://api.minimaxi.com/anthropic/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "sk-test-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andRespond(withSuccess(
                        "{\"content\":[{\"type\":\"text\",\"text\":\"SELECT 1;\"}]}",
                        MediaType.APPLICATION_JSON));

        String sql = client.chat("你是 SQL 生成器", "查一下销售额");

        server.verify();
        assertThat(sql).isEqualTo("SELECT 1;");
    }

    @Test
    @DisplayName("chat 对 thinking+text 多 content 应正确取 text 片段")
    void shouldParseTextContentWhenThinkingPresent() {
        server.expect(requestTo("https://api.minimaxi.com/anthropic/v1/messages"))
                .andRespond(withSuccess(
                        "{\"content\":[{\"type\":\"thinking\",\"thinking\":\"...\",\"signature\":\"sig\"},{\"type\":\"text\",\"text\":\"SELECT 2;\"}]}",
                        MediaType.APPLICATION_JSON));

        String sql = client.chat("你是 SQL 生成器", "查一下销售额");

        assertThat(sql).isEqualTo("SELECT 2;");
    }

    @Test
    @DisplayName("chat 收到空 content 应抛 IllegalStateException")
    void shouldThrowOnEmptyContent() {
        server.expect(requestTo("https://api.minimaxi.com/anthropic/v1/messages"))
                .andRespond(withSuccess("{\"content\":[]}", MediaType.APPLICATION_JSON));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.chat("s", "u"))
                .isInstanceOf(IllegalStateException.class);
    }
}
