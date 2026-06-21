package com.nl2sql.schema.exception;

import com.nl2sql.common.enums.IResultCode;

/**
 * schema-service 领域错误码。编码用 11xxx band，避开 common {@code ResultCode} 的 HTTP 语义区间。
 */
public enum SchemaResultCode implements IResultCode {

    DB_TYPE_UNSUPPORTED(11001, "schema.db_type_unsupported", "不支持的数据库类型"),
    DATASOURCE_NOT_FOUND(11002, "schema.datasource_not_found", "数据源不存在"),
    SCAN_CONNECT_FAILED(11003, "schema.scan_connect_failed", "无法连接目标数据库"),
    SCAN_EXECUTE_FAILED(11004, "schema.scan_execute_failed", "扫描执行失败");

    private final Integer code;
    private final String i18nKey;
    private final String desc;

    SchemaResultCode(Integer code, String i18nKey, String desc) {
        this.code = code;
        this.i18nKey = i18nKey;
        this.desc = desc;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @Override
    public String getI18nKey() {
        return i18nKey;
    }
}
