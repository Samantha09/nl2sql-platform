package com.nl2sql.schema.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 映射 table_list_cache：每数据源一行，表名按数据库分组以 JSON Map 存储。 */
@Data
@Entity
@Table(name = "table_list_cache")
public class TableListCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属数据源 id */
    @Column(name = "data_source_id", nullable = false)
    private Long dataSourceId;

    /** 表名分组 JSON（Map&lt;databaseName, List&lt;tableName&gt;&gt; 序列化） */
    @Column(name = "table_json", columnDefinition = "TEXT")
    private String tableJson;

    /** 最近一次扫描写入时间 */
    @Column(name = "cached_at")
    private LocalDateTime cachedAt;
}
