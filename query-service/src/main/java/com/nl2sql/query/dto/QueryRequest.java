package com.nl2sql.query.dto;

import lombok.Data;

@Data
public class QueryRequest {
    private Long userId;
    private Long dataSourceId;
    private String naturalLanguage;
    private String conversationId;
}
