package com.nl2sql.common.mq;

/**
 * 消息队列拓扑常量集中定义，消除各服务间 exchange/queue/routingKey 的重复声明。
 * <p>每组包含 exchange、queue、routingKey，以及对应的死信（DLX/DLQ）命名。
 * 死信命名规则：在原名后加 {@code .dlx} / {@code .dlq} / {@code .dlk} 后缀。
 */
public final class MqConst {

    private MqConst() {
    }

    /** 死信交换机后缀 */
    public static final String DLX_SUFFIX = ".dlx";
    /** 死信队列后缀 */
    public static final String DLQ_SUFFIX = ".dlq";
    /** 死信路由键后缀 */
    public static final String DLK_SUFFIX = ".dlk";

    /** NL→SQL 请求事件：query-service 生产，ai-service 消费 */
    public static final class Nl2Sql {
        public static final String EXCHANGE = "nl2sql.exchange";
        public static final String QUEUE = "nl2sql.queue";
        public static final String ROUTING_KEY = "nl2sql.event";

        public static final String DLX = EXCHANGE + DLX_SUFFIX;
        public static final String DLQ = QUEUE + DLQ_SUFFIX;
        public static final String DLK = ROUTING_KEY + DLK_SUFFIX;
    }

    /** SQL 生成完成事件：ai-service 生产，query-service 消费 */
    public static final class SqlReady {
        public static final String EXCHANGE = "sql.ready.exchange";
        public static final String QUEUE = "sql.ready.queue";
        public static final String ROUTING_KEY = "sql.ready.event";

        public static final String DLX = EXCHANGE + DLX_SUFFIX;
        public static final String DLQ = QUEUE + DLQ_SUFFIX;
        public static final String DLK = ROUTING_KEY + DLK_SUFFIX;
    }

    /** 结果待可视化事件：query-service 内部流转 */
    public static final class ResultReady {
        public static final String EXCHANGE = "result.ready.exchange";
        public static final String QUEUE = "result.ready.queue";
        public static final String ROUTING_KEY = "result.ready.event";

        public static final String DLX = EXCHANGE + DLX_SUFFIX;
        public static final String DLQ = QUEUE + DLQ_SUFFIX;
        public static final String DLK = ROUTING_KEY + DLK_SUFFIX;
    }
}
