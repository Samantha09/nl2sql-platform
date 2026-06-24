package com.nl2sql.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 精简 schema 上下文，供 LLM 生成 SQL。由 schema-service 从已持久化的 schema_cache 组装。
 */
@Data
public class SchemaContextDTO implements Serializable {

    /** 库名 */
    private String database;

    /** 表清单 */
    private List<TableBrief> tables = new ArrayList<>();

    /** 单表摘要 */
    @Data
    public static class TableBrief implements Serializable {
        /** 表名 */
        private String tableName;
        /** 表注释 */
        private String tableComment;
        /** 列清单 */
        private List<ColumnBrief> columns = new ArrayList<>();
    }

    /** 列摘要 */
    @Data
    public static class ColumnBrief implements Serializable {
        /** 列名 */
        private String name;
        /** 方言原文类型，如 varchar(255) */
        private String type;
        /** 列注释 */
        private String comment;
    }
}
