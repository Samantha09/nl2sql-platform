package com.nl2sql.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI 转换结果。区分 SQL 生成与自然语言交互意图。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConvertResponse implements Serializable {

    /** 响应类型：sql / chat / clarification */
    private String type;

    /** 内容：SQL 文本或自然语言回复 */
    private String content;

    public static ConvertResponse sql(String sql) {
        return new ConvertResponse("sql", sql);
    }

    public static ConvertResponse chat(String message) {
        return new ConvertResponse("chat", message);
    }

    public static ConvertResponse clarification(String hint) {
        return new ConvertResponse("clarification", hint);
    }
}
