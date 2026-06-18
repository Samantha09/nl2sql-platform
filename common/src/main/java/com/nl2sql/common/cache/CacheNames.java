package com.nl2sql.common.cache;

/**
 * 缓存名常量集中定义，避免魔法字符串散落各处。
 * <p>每个常量对应一个逻辑缓存区，可在 {@link CacheProperties#getTtls()} 中为其单独配置 TTL。
 * 新增缓存时：此处加常量 → 按需在 {@link CacheTtl} 配默认 TTL → 业务方法 {@code @Cacheable(cacheNames = ...)} 引用。
 */
public final class CacheNames {

    private CacheNames() {
    }

    /** 数据源列表（schema-service） */
    public static final String DS_LIST = "ds:list";

    /** 单张表结构详情（schema-service） */
    public static final String SCHEMA_TABLE = "schema:table";

    /** 表名列表（schema-service） */
    public static final String SCHEMA_TABLES = "schema:tables";

    /** 查询历史（query-service） */
    public static final String QUERY_HISTORY = "query:history";
}
