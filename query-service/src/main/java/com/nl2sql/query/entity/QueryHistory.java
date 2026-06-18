package com.nl2sql.query.entity;

import com.nl2sql.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "query_history")
public class QueryHistory extends BaseEntity {

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "data_source_id", nullable = false)
    private Long dataSourceId;

    @Column(name = "natural_language", nullable = false, length = 1000)
    private String naturalLanguage;

    @Column(name = "generated_sql", nullable = false, columnDefinition = "TEXT")
    private String generatedSql;

    @Column(name = "sql_executed", columnDefinition = "TEXT")
    private String sqlExecuted;

    @Column(name = "execute_time_ms")
    private Long executeTimeMs = 0L;

    @Column(name = "result_count")
    private Integer resultCount = 0;

    @Column(name = "status", length = 20)
    private String status = "success";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "chart_type", length = 20)
    private String chartType = "table";
}
