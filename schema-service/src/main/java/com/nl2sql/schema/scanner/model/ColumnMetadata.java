package com.nl2sql.schema.scanner.model;

import lombok.Data;

/** 列结构（方言无关）。 */
@Data
public class ColumnMetadata {

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

    /** 列在表中的序号（从 1 起） */
    private int ordinalPosition;
}
