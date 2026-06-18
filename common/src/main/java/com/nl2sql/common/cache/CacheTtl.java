package com.nl2sql.common.cache;

import java.time.Duration;
import java.util.Map;

/**
 * 各缓存区的默认 TTL，与 {@link CacheNames} 一一对应。
 * <p>这是代码内置的兜底默认值；运行期可通过 {@code nl2sql.cache.ttls.<cacheName>} 覆盖
 * （见 {@link CacheProperties#getTtls()}）。新增缓存时在 {@link #DEFAULTS} 补一行即可。
 */
public final class CacheTtl {

    private CacheTtl() {
    }

    /** 全局默认 TTL：未在 {@link #DEFAULTS} 也未在配置中指定的缓存区使用此值 */
    public static final Duration DEFAULT = Duration.ofMinutes(10);

    /** 空值缓存 TTL：防穿透，命中空结果时用较短过期时间 */
    public static final Duration NULL_VALUE = Duration.ofSeconds(60);

    /** 各缓存区内置默认 TTL */
    public static final Map<String, Duration> DEFAULTS = Map.of(
            CacheNames.DS_LIST, Duration.ofMinutes(5),
            CacheNames.SCHEMA_TABLE, Duration.ofMinutes(30),
            CacheNames.SCHEMA_TABLES, Duration.ofMinutes(10),
            CacheNames.QUERY_HISTORY, Duration.ofMinutes(2)
    );
}
