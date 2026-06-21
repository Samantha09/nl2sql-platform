package com.nl2sql.schema.dto;

import lombok.Data;

import java.util.List;

/** 单表结构详情，供前端展示与 AI 生成 SQL 使用。由 schema_cache 的 JSON 列反序列化组装。 */
@Data
public class TableSchemaDTO {

    /** 表名 */
    private String tableName;

    /** 表注释 */
    private String tableComment;

    /** 列清单，按序号排列 */
    private List<ColumnInfo> columns;

    /** 主键列名，按序 */
    private List<String> primaryKeys;

    /** 索引清单 */
    private List<IndexInfo> indexes;

    /** 外键清单 */
    private List<ForeignKeyInfo> foreignKeys;

    /** 列信息 */
    @Data
    public static class ColumnInfo {
        /** 列名 */
        private String name;
        /** 方言原文类型，如 varchar(255) */
        private String type;
        /** 列注释 */
        private String comment;
        /** 是否可空 */
        private boolean nullable;
        /** 默认值，无则为 null */
        private String defaultValue;
    }

    /** 索引信息 */
    @Data
    public static class IndexInfo {
        /** 索引名 */
        private String name;
        /** 是否唯一索引 */
        private boolean unique;
        /** 组成列，按索引内顺序 */
        private List<String> columns;
    }

    /** 外键信息：本表 columns 一一对应 referencedTable.referencedColumns */
    @Data
    public static class ForeignKeyInfo {
        /** 外键约束名 */
        private String name;
        /** 本表参与外键的列 */
        private List<String> columns;
        /** 被引用的表名 */
        private String referencedTable;
        /** 被引用表的列，与 columns 一一对应 */
        private List<String> referencedColumns;
    }
}
