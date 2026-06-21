package com.nl2sql.schema.scanner.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 外键（方言无关）：本表 columns 一一对应 referencedTable.referencedColumns。 */
@Data
public class ForeignKeyMetadata {

    /** 外键约束名 */
    private String name;

    /** 本表参与外键的列 */
    private List<String> columns = new ArrayList<>();

    /** 被引用的表名 */
    private String referencedTable;

    /** 被引用表的列，与 columns 一一对应 */
    private List<String> referencedColumns = new ArrayList<>();
}
