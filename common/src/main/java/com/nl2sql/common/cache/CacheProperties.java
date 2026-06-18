package com.nl2sql.common.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存可配置项，前缀 {@code nl2sql.cache}。作为缓存层的统一扩展入口：
 * 调整 TTL、前缀、空值策略，以及后续开启 L2 二级缓存，均只改配置或本类，业务方法注解不变。
 *
 * <pre>
 * nl2sql:
 *   cache:
 *     key-prefix: "nl2sql:"
 *     default-ttl: 10m
 *     null-ttl: 60s
 *     cache-null-values: true
 *     ttls:
 *       schema:table: 30m
 *       ds:list: 5m
 *     multi-level:
 *       enabled: false          # L2 开关（预留，本期未实现）
 *       local-ttls:
 *         schema:table: 2m      # L1 本地缓存 TTL（预留）
 * </pre>
 */
@ConfigurationProperties(prefix = "nl2sql.cache")
public class CacheProperties {

    /** Redis key 统一前缀，便于隔离与按前缀清理 */
    private String keyPrefix = "nl2sql:";

    /** 全局默认 TTL，未在 {@link #ttls} 指定的缓存区使用此值 */
    private Duration defaultTtl = CacheTtl.DEFAULT;

    /** 空值缓存 TTL，配合 {@link #cacheNullValues} 防穿透 */
    private Duration nullTtl = CacheTtl.NULL_VALUE;

    /** 是否缓存空值（null）以抵御缓存穿透 */
    private boolean cacheNullValues = true;

    /** 按 cacheName 覆盖 TTL；优先级高于 {@link CacheTtl#DEFAULTS} 与 {@link #defaultTtl} */
    private Map<String, Duration> ttls = new HashMap<>();

    /** L2 二级缓存配置（预留，本期未启用） */
    private MultiLevel multiLevel = new MultiLevel();

    /**
     * 二级缓存（Caffeine L1 + Redis L2）扩展位。
     * 本期不实现，仅占位：将来 {@code enabled=true} 时由 CompositeCacheManager 接管，
     * 调用方 {@code @Cacheable} 代码无需改动。
     */
    public static class MultiLevel {
        /** 是否启用 L1 本地缓存 */
        private boolean enabled = false;
        /** 各缓存区的 L1 本地 TTL（通常远短于 L2，以降低多实例不一致窗口） */
        private Map<String, Duration> localTtls = new HashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, Duration> getLocalTtls() {
            return localTtls;
        }

        public void setLocalTtls(Map<String, Duration> localTtls) {
            this.localTtls = localTtls;
        }
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Duration getNullTtl() {
        return nullTtl;
    }

    public void setNullTtl(Duration nullTtl) {
        this.nullTtl = nullTtl;
    }

    public boolean isCacheNullValues() {
        return cacheNullValues;
    }

    public void setCacheNullValues(boolean cacheNullValues) {
        this.cacheNullValues = cacheNullValues;
    }

    public Map<String, Duration> getTtls() {
        return ttls;
    }

    public void setTtls(Map<String, Duration> ttls) {
        this.ttls = ttls;
    }

    public MultiLevel getMultiLevel() {
        return multiLevel;
    }

    public void setMultiLevel(MultiLevel multiLevel) {
        this.multiLevel = multiLevel;
    }
}
