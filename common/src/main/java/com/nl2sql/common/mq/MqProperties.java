package com.nl2sql.common.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 消息队列可配置项，前缀 {@code nl2sql.mq}。集中管理消费重试与死信策略，
 * 调整重试次数 / 退避时间无需改代码。
 *
 * <pre>
 * nl2sql:
 *   mq:
 *     max-retries: 3            # 消费失败最大重试次数，超限进 DLQ
 *     initial-interval: 1000    # 首次重试间隔(ms)
 *     multiplier: 2.0           # 退避倍数
 *     max-interval: 10000       # 最大重试间隔(ms)
 * </pre>
 */
@ConfigurationProperties(prefix = "nl2sql.mq")
public class MqProperties {

    /** 消费失败最大重试次数，超限投递死信队列 */
    private int maxRetries = 3;

    /** 首次重试间隔（毫秒） */
    private long initialInterval = 1000L;

    /** 退避倍数 */
    private double multiplier = 2.0;

    /** 最大重试间隔（毫秒） */
    private long maxInterval = 10000L;

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getInitialInterval() {
        return initialInterval;
    }

    public void setInitialInterval(long initialInterval) {
        this.initialInterval = initialInterval;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public long getMaxInterval() {
        return maxInterval;
    }

    public void setMaxInterval(long maxInterval) {
        this.maxInterval = maxInterval;
    }
}
