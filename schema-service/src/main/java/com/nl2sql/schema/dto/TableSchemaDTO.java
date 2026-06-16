package com.nl2sql.schema.dto;

import lombok.Data;

import java.util.List;

@Data
public class TableSchemaDTO {
    private String tableName;
    private String tableComment;
    private List<ColumnInfo> columns;
    private List<String> primaryKeys;

    @Data
    public static class ColumnInfo {
        private String name;
        private String type;
        private String comment;
    }
}
