package com.nl2sql.schema.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 映射 schema_cache：每(数据源,表)一行，结构以 JSON 列存储。 */
@Data
@Entity
@Table(name = "schema_cache",
        uniqueConstraints = @UniqueConstraint(name = "uk_ds_table",
                columnNames = {"data_source_id", "table_name"}))
public class SchemaCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属数据源 id */
    @Column(name = "data_source_id", nullable = false)
    private Long dataSourceId;

    /** 表名 */
    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    /** 表注释 */
    @Column(name = "table_comment", length = 500)
    private String tableComment;

    /** 列清单 JSON（List&lt;ColumnMetadata&gt; 序列化） */
    @Column(name = "column_json", columnDefinition = "TEXT")
    private String columnJson;

    /** 主键列名 JSON（List&lt;String&gt; 序列化） */
    @Column(name = "primary_key_json", columnDefinition = "TEXT")
    private String primaryKeyJson;

    /** 外键清单 JSON（List&lt;ForeignKeyMetadata&gt; 序列化） */
    @Column(name = "foreign_key_json", columnDefinition = "TEXT")
    private String foreignKeyJson;

    /** 索引清单 JSON（List&lt;IndexMetadata&gt; 序列化） */
    @Column(name = "index_json", columnDefinition = "TEXT")
    private String indexJson;

    /** 行数估算 */
    @Column(name = "row_estimate")
    private Long rowEstimate;

    /** 最近一次扫描写入时间 */
    @Column(name = "cached_at")
    private LocalDateTime cachedAt;

    /** 版本号，每次重扫描该表 +1 */
    @Column(name = "version")
    private Integer version;
}
