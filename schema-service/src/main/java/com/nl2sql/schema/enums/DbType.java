package com.nl2sql.schema.enums;

import com.nl2sql.common.enums.IEnum;

/**
 * 支持的数据库类型。code 即 {@code DataSourceConfig.type} 的取值。
 * <p>新增方言：此处加枚举值 → 提供对应 {@code DatabaseScanner} 实现 → 补 JDBC 驱动依赖。
 */
public enum DbType implements IEnum<String> {

    MYSQL("mysql", "MySQL");

    private final String code;
    private final String desc;

    DbType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
