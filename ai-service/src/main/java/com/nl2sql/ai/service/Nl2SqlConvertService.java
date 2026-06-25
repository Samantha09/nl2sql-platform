package com.nl2sql.ai.service;

import com.nl2sql.ai.config.LlmProperties;
import com.nl2sql.ai.exception.AiResultCode;
import com.nl2sql.ai.llm.IntentDetector;
import com.nl2sql.ai.llm.LlmClient;
import com.nl2sql.ai.llm.SchemaContextBuilder;
import com.nl2sql.common.dto.ConvertRequest;
import com.nl2sql.common.dto.ConvertResponse;
import com.nl2sql.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** NL → SQL 整合：先判断意图 → 取 schema 上下文 → 调 LLM → 失败按配置降级或报错。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Nl2SqlConvertService {

    private final LlmClient llmClient;
    private final SchemaContextBuilder schemaContextBuilder;
    private final MockLLMService mockLLMService;
    private final LlmProperties properties;
    private final IntentDetector intentDetector;

    public ConvertResponse convert(ConvertRequest request) {
        try {
            // 1. 意图识别
            IntentDetector.Result intent = intentDetector.detect(request.getNaturalLanguage());
            if (intent.intent() != IntentDetector.Intent.QUERY) {
                return ConvertResponse.chat(intent.response());
            }

            // 2. 生成 SQL
            String context = schemaContextBuilder.build(request.getDataSourceId(), request.getDatabaseName());
            String sql = cleanSql(llmClient.chat(context, request.getNaturalLanguage()));
            return ConvertResponse.sql(sql);
        } catch (Exception e) {
            if (properties.isFallbackToMock()) {
                log.warn("LLM 调用失败，降级到 MockLLMService: {}", e.getMessage());
                return ConvertResponse.sql(mockLLMService.convert(request.getNaturalLanguage(), request.getDataSourceId()));
            }
            throw new BaseException(AiResultCode.AI_LLM_UNAVAILABLE,
                    AiResultCode.AI_LLM_UNAVAILABLE.getMessage(), e);
        }
    }

    /** 清理 LLM 可能包裹的 markdown 代码块标记，只保留纯 SQL。 */
    private String cleanSql(String sql) {
        if (sql == null) {
            return "";
        }
        String cleaned = sql.strip();
        // 去掉开头的 ```sql 或 ```
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            cleaned = firstNewline > 0 ? cleaned.substring(firstNewline + 1) : cleaned.substring(3);
        }
        // 去掉结尾的 ```
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.strip();
    }
}
