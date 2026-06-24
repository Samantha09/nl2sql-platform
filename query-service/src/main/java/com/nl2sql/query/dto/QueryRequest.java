package com.nl2sql.query.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QueryRequest {
    private Long userId;
    private Long dataSourceId;

    @NotBlank(message = "自然语言查询内容不能为空")
    private String naturalLanguage;

    private String conversationId;

    /** 目标库名：缺省时按数据源唯一库解析，多库必填 */
    private String databaseName;
}
