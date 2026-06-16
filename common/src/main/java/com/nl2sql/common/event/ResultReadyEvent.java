package com.nl2sql.common.event;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class ResultReadyEvent implements Serializable {
    private String eventId;
    private String sqlRequestId;
    private List<Map<String, Object>> data;
    private Integer totalCount;
    private Long executeTimeMs;
    private Long timestamp;
}
