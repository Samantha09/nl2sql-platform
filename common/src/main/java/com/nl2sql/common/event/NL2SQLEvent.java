package com.nl2sql.common.event;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class NL2SQLEvent implements Serializable {
    private String eventId;
    private Long userId;
    private Long dataSourceId;
    private String naturalLanguage;
    private String conversationId;
    private Map<String, Object> schemaContext;
    private Long timestamp;
}
