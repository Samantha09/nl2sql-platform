package com.nl2sql.ai.llm;

import com.nl2sql.ai.config.LlmProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages 兼容端点实现（MiniMax）。POST {baseUrl}/v1/messages，
 * 携带 x-api-key + anthropic-version，响应取 content[0].text。
 */
@Component
public class AnthropicMessagesLlmClient implements LlmClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final LlmProperties properties;

    public AnthropicMessagesLlmClient(RestClient llmRestClient, LlmProperties properties) {
        this.restClient = llmRestClient;
        this.properties = properties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "max_tokens", properties.getMaxTokens(),
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );
        Map<String, Object> resp = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", properties.getApiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .retrieve()
                .body(Map.class);
        if (resp == null) {
            throw new IllegalStateException("LLM 响应为空");
        }
        List<Map<String, Object>> content = (List<Map<String, Object>>) resp.get("content");
        if (content == null || content.isEmpty()) {
            throw new IllegalStateException("LLM 响应 content 为空");
        }
        Object text = content.get(0).get("text");
        if (text == null) {
            throw new IllegalStateException("LLM 响应 content[0].text 为空");
        }
        return text.toString();
    }
}
