package com.nl2sql.query.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QueryResult {
    private String sql;
    private List<Map<String, Object>> data;
    private Integer totalCount;
    private Long executeTimeMs;
    private String chartType;
}
