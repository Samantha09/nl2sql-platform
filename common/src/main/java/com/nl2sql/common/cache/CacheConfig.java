package com.nl2sql.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存自动装配：基于 Spring Cache + Redis 的增强配置。
 * <ul>
 *   <li>value 用 JSON 序列化（可读、跨语言），key 用字符串序列化</li>
 *   <li>统一 key 前缀（{@code nl2sql:}），每个 cacheName 可配独立 TTL</li>
 *   <li>缓存空值防穿透，空值用较短 TTL</li>
 *   <li>预留 L2 二级缓存扩展点（见 {@link #cacheManager}）</li>
 * </ul>
 * 仅在 classpath 存在 Redis 时生效，对纯 DTO 使用者（如仅依赖 common 的模块）无副作用。
 */
@AutoConfiguration
@EnableCaching
@ConditionalOnClass({RedisConnectionFactory.class, RedisCacheManager.class})
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    /** JSON 序列化器：value 以 JSON 存储，保留类型信息以便正确反序列化 */
    private GenericJackson2JsonRedisSerializer jsonSerializer() {
        return new GenericJackson2JsonRedisSerializer(new ObjectMapper());
    }

    /** 通用 RedisTemplate：String key + JSON value，供需要手动操作缓存的场景使用 */
    @Bean
    @ConditionalOnMissingBean(name = "cacheRedisTemplate")
    public RedisTemplate<String, Object> cacheRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer());
        template.setHashValueSerializer(jsonSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /** 单个缓存区的基础配置：前缀、TTL、空值策略、序列化器 */
    private RedisCacheConfiguration baseConfig(CacheProperties props, Duration ttl) {
        RedisCacheConfiguration cfg = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(props.getKeyPrefix())
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer()));
        // 默认开启空值缓存防穿透；关闭时禁用
        return props.isCacheNullValues() ? cfg : cfg.disableCachingNullValues();
    }

    /** 解析某缓存区的 TTL：配置覆盖 > 内置默认 > 全局默认 */
    private Duration resolveTtl(CacheProperties props, String cacheName) {
        Duration fromConfig = props.getTtls().get(cacheName);
        if (fromConfig != null) {
            return fromConfig;
        }
        return CacheTtl.DEFAULTS.getOrDefault(cacheName, props.getDefaultTtl());
    }

    /**
     * 缓存管理器（L1 单层 Redis）。
     * <p><b>L2 扩展点</b>：当 {@code nl2sql.cache.multi-level.enabled=true} 时，
     * 未来在此再提供一个 {@code CompositeCacheManager}（Caffeine L1 + 本 Redis L2）的
     * {@code @Bean}，并用 {@code @ConditionalOnProperty} 互斥切换即可，本方法与调用方注解均无需改动。
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(RedisConnectionFactory factory, CacheProperties props) {
        // 已知缓存区按各自 TTL 预置，未预置的缓存区由 default 兜底
        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        for (String cacheName : CacheTtl.DEFAULTS.keySet()) {
            perCache.put(cacheName, baseConfig(props, resolveTtl(props, cacheName)));
        }
        props.getTtls().forEach((cacheName, ttl) ->
                perCache.put(cacheName, baseConfig(props, ttl)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(baseConfig(props, props.getDefaultTtl()))
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
