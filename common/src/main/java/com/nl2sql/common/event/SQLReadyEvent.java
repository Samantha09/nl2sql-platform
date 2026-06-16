package com.nl2sql.common.event;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SQLReadyEvent implements Serializable {
    private String eventId;
    private String nlRequestId;
    private String generatedSql;
    private Boolean sqlValid;
    private List<String> validationErrors;
    private Long timestamp;
}
