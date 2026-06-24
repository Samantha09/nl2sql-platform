package com.nl2sql.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * LLM 可配置项，前缀 {@code nl2sql.llm}。MiniMax 走 Anthropic Messages 兼容端点。
 *
 * <pre>
 * nl2sql:
 *   llm:
 *     base-url: https://api.minimaxi.com/anthropic
 *     api-key: ENC(...)          # 或 ${MINIMAX_API_KEY}
 *     model: MiniMax-M2.7
 *     max-tokens: 1024
 *     timeout: 30s
 *     fallback-to-mock: false    # LLM 失败时是否降级到 MockLLMService
 * </pre>
 */
@ConfigurationProperties(prefix = "nl2sql.llm")
public class LlmProperties {

    /** Anthropic Messages 兼容端点 baseUrl */
    private String baseUrl = "https://api.minimaxi.com/anthropic";

    /** API Key，支持 ENC(...) 加密密文（启动时自动解密） */
    private String apiKey = "";

    /** 模型名 */
    private String model = "MiniMax-M2.7";

    /** 最大生成 token 数 */
    private int maxTokens = 1024;

    /** 请求超时 */
    private Duration timeout = Duration.ofSeconds(30);

    /** LLM 失败时是否降级到 MockLLMService */
    private boolean fallbackToMock = false;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public boolean isFallbackToMock() { return fallbackToMock; }
    public void setFallbackToMock(boolean fallbackToMock) { this.fallbackToMock = fallbackToMock; }
}
