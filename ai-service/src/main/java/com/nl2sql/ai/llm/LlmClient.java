package com.nl2sql.ai.llm;

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
}
