package com.nl2sql.ai.service;

import com.nl2sql.ai.config.LlmProperties;
import com.nl2sql.ai.exception.AiResultCode;
import com.nl2sql.ai.llm.LlmClient;
import com.nl2sql.ai.llm.SchemaContextBuilder;
import com.nl2sql.common.dto.ConvertRequest;
import com.nl2sql.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** NL → SQL 整合：取 schema 上下文 → 调 LLM → 失败按配置降级或报错。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Nl2SqlConvertService {

    private final LlmClient llmClient;
    private final SchemaContextBuilder schemaContextBuilder;
    private final MockLLMService mockLLMService;
    private final LlmProperties properties;

    public String convert(ConvertRequest request) {
        try {
            String context = schemaContextBuilder.build(request.getDataSourceId(), request.getDatabaseName());
            return llmClient.chat(context, request.getNaturalLanguage());
        } catch (Exception e) {
            if (properties.isFallbackToMock()) {
                log.warn("LLM 调用失败，降级到 MockLLMService: {}", e.getMessage());
                return mockLLMService.convert(request.getNaturalLanguage(), request.getDataSourceId());
            }
            throw new BaseException(AiResultCode.AI_LLM_UNAVAILABLE,
                    AiResultCode.AI_LLM_UNAVAILABLE.getMessage(), e);
        }
    }
}
