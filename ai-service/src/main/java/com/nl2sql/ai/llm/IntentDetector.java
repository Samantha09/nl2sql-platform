package com.nl2sql.ai.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.ai.config.LlmProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 用户意图识别器。先判断输入是数据查询、闲聊还是意图不明，
 * 仅当意图为 {@code query} 时才进入 SQL 生成阶段。
 *
 * <p>优先使用 Anthropic function calling；当 provider 不支持时 fallback 到结构化 JSON prompt。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentDetector {

    private static final String SYSTEM_PROMPT = """
            你是 NL2SQL 平台的意图识别助手。请判断用户输入的意图：
            - query：用户想查询数据库，需要生成 SQL
            - chat：用户在闲聊、打招呼、提问与数据库无关的内容
            - clarification：用户意图模糊，需要进一步澄清

            如果是 chat 或 clarification，请直接给出友好、简洁的中文回复。
            """;

    private final LlmClient llmClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public enum Intent { QUERY, CHAT, CLARIFICATION }

    public record Result(Intent intent, String response) {
    }

    /**
     * 识别用户意图。
     *
     * @param naturalLanguage 用户输入
     * @return 意图与（当非 query 时的）回复文本
     */
    public Result detect(String naturalLanguage) {
        try {
            return detectByFunctionCall(naturalLanguage);
        } catch (Exception e) {
            log.warn("function calling 意图识别失败，fallback 到结构化 prompt: {}", e.getMessage());
            return detectByStructuredPrompt(naturalLanguage);
        }
    }

    private Result detectByFunctionCall(String naturalLanguage) {
        List<Map<String, Object>> tools = List.of(Map.of(
                "name", "detect_intent",
                "description", "判断用户输入的意图并给出回应",
                "input_schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "intent", Map.of(
                                        "type", "string",
                                        "enum", List.of("query", "chat", "clarification"),
                                        "description", "用户意图"
                                ),
                                "response", Map.of(
                                        "type", "string",
                                        "description", "当 intent 为 chat 或 clarification 时，给用户的友好中文回复；当 intent 为 query 时可留空"
                                )
                        ),
                        "required", List.of("intent", "response")
                )
        ));
        Map<String, String> toolChoice = Map.of("type", "tool", "name", "detect_intent");

        Map<String, Object> input = llmClient.chatWithTools(SYSTEM_PROMPT, naturalLanguage, tools, toolChoice);
        String intentStr = (String) input.get("intent");
        String response = (String) input.getOrDefault("response", "");
        return parseResult(intentStr, response);
    }

    private Result detectByStructuredPrompt(String naturalLanguage) {
        String prompt = naturalLanguage + """

                请判断上面这句话的意图，只返回 JSON，不要任何解释：
                {"intent": "query|chat|clarification", "response": "给用户的回复（非query时填写）"}
                """;
        String text = llmClient.chat(SYSTEM_PROMPT, prompt);
        try {
            Map<String, Object> map = objectMapper.readValue(text, Map.class);
            String intentStr = (String) map.get("intent");
            String response = (String) map.getOrDefault("response", "");
            return parseResult(intentStr, response);
        } catch (JsonProcessingException e) {
            log.warn("结构化意图识别 JSON 解析失败: {}", text);
            return new Result(Intent.CLARIFICATION, "抱歉，我没理解您的意思，请尝试描述您想查询的数据。");
        }
    }

    private Result parseResult(String intentStr, String response) {
        if (intentStr == null) {
            return new Result(Intent.CLARIFICATION, "抱歉，我没理解您的意思，请尝试描述您想查询的数据。");
        }
        return switch (intentStr.toLowerCase()) {
            case "query" -> new Result(Intent.QUERY, null);
            case "chat" -> new Result(Intent.CHAT, isBlank(response)
                    ? "你好！我是 AI 数据库助手，可以帮你把自然语言转成 SQL 查询。" : response);
            default -> new Result(Intent.CLARIFICATION, isBlank(response)
                    ? "请问您想查询什么数据？可以描述一下表名或字段。" : response);
        };
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
