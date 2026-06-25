package com.nl2sql.ai.llm;

import java.util.List;
import java.util.Map;

/** LLM 抽象，便于切换 provider 或在测试中替换为 mock。 */
public interface LlmClient {

    /**
     * 对话补全。
     *
     * @param systemPrompt 系统提示（schema 上下文 + 角色约束）
     * @param userPrompt   用户自然语言问句
     * @return LLM 生成的文本（期望为 SQL）
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 带 function calling / tools 的对话补全。
     *
     * @param systemPrompt 系统提示
     * @param userPrompt   用户自然语言问句
     * @param tools        Anthropic tools 定义列表
     * @param toolChoice   强制使用的 tool 名称，例如 {"type": "tool", "name": "detect_intent"}
     * @return LLM 返回的 tool_use 调用参数 Map（通常含 name 和 input）
     */
    Map<String, Object> chatWithTools(String systemPrompt, String userPrompt,
                                      List<Map<String, Object>> tools,
                                      Map<String, String> toolChoice);
}
