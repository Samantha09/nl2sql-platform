package com.nl2sql.ai.exception;

import com.nl2sql.common.enums.IResultCode;

/**
 * ai-service 领域错误码。编码用 12xxx band，避开 common HTTP 语义区间与其他服务 band。
 */
public enum AiResultCode implements IResultCode {

    AI_LLM_UNAVAILABLE(12001, "ai.llm_unavailable", "AI 服务暂不可用");

    private final Integer code;
    private final String i18nKey;
    private final String desc;

    AiResultCode(Integer code, String i18nKey, String desc) {
        this.code = code;
        this.i18nKey = i18nKey;
        this.desc = desc;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @Override
    public String getI18nKey() {
        return i18nKey;
    }
}
