package com.nl2sql.schema.scanner.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 索引（方言无关）。 */
@Data
public class IndexMetadata {

    /** 索引名 */
    private String name;

    /** 是否唯一索引 */
    private boolean unique;

    /** 组成列，按索引内顺序 */
    private List<String> columns = new ArrayList<>();
}
