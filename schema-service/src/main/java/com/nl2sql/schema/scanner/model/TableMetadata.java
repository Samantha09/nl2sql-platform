package com.nl2sql.schema.scanner.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 单表的完整结构（方言无关）。 */
@Data
public class TableMetadata {

    /** 表名 */
    private String name;

    /** 表注释 */
    private String comment;

    /** 行数估算（来自方言的统计信息，非精确值） */
    private long rowEstimate;

    /** 列清单，按序号排列 */
    private List<ColumnMetadata> columns = new ArrayList<>();

    /** 主键列名，按序 */
    private List<String> primaryKeys = new ArrayList<>();

    /** 索引清单 */
    private List<IndexMetadata> indexes = new ArrayList<>();

    /** 外键清单 */
    private List<ForeignKeyMetadata> foreignKeys = new ArrayList<>();
}
