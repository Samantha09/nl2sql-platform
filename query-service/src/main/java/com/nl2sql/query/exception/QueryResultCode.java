package com.nl2sql.query.exception;

import com.nl2sql.common.enums.IResultCode;

/**
 * query-service 领域错误码。编码用 13xxx band。
 */
public enum QueryResultCode implements IResultCode {

    QUERY_DB_TYPE_UNSUPPORTED(13001, "query.db_type_unsupported", "暂不支持该数据库类型"),
    QUERY_DATASOURCE_CONNECT_FAILED(13002, "query.datasource_connect_failed", "无法连接目标数据库"),
    QUERY_SQL_EXECUTE_FAILED(13003, "query.sql_execute_failed", "SQL 执行失败"),
    QUERY_DATABASE_NOT_SPECIFIED(13004, "query.database_not_specified", "数据源含多个库，需指定 databaseName");

    private final Integer code;
    private final String i18nKey;
    private final String desc;

    QueryResultCode(Integer code, String i18nKey, String desc) {
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
