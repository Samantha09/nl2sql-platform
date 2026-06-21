package com.nl2sql.schema.scanner;

import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.scanner.model.SchemaMetadata;

import java.util.List;

/** 数据库结构扫描 SPI：一种数据库一份实现。 */
public interface DatabaseScanner {

    /** 该实现负责哪种数据库。 */
    boolean supports(DbType type);

    /** 全量扫描：表 + 列 + 主键 + 外键 + 索引。 */
    SchemaMetadata scan(ScanContext context);

    /** 轻量：仅列出表名。 */
    List<String> listTables(ScanContext context);
}
