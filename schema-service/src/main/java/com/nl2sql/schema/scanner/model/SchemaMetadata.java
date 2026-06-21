package com.nl2sql.schema.scanner.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 一次扫描的完整结果（方言无关）。 */
@Data
public class SchemaMetadata {

    /** 扫描到的全部表 */
    private List<TableMetadata> tables = new ArrayList<>();
}
