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
        // MiniMax 等兼容端点会在 content 里先返回 type=thinking 的推理片段，
        // 真正的文本答案在 type=text 的元素中，需要按类型过滤而非直接取第一个。
        Object text = content.stream()
                .filter(c -> "text".equals(c.get("type")))
                .findFirst()
                .map(c -> c.get("text"))
                .orElseThrow(() -> new IllegalStateException("LLM 响应中未找到 text 类型 content"));
        return text.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> chatWithTools(String systemPrompt, String userPrompt,
                                           List<Map<String, Object>> tools,
                                           Map<String, String> toolChoice) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", properties.getModel());
        body.put("max_tokens", properties.getMaxTokens());
        body.put("system", systemPrompt);
        body.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));
        body.put("tools", tools);
        body.put("tool_choice", toolChoice);

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
        return content.stream()
                .filter(c -> "tool_use".equals(c.get("type")))
                .findFirst()
                .map(c -> (Map<String, Object>) c.get("input"))
                .orElseThrow(() -> new IllegalStateException("LLM 响应中未找到 tool_use"));
    }
}
