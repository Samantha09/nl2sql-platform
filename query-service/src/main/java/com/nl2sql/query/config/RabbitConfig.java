package com.nl2sql.query.config;

import com.nl2sql.common.mq.MqConst;
import com.nl2sql.common.mq.MqTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * query-service 生产 nl2sql 事件、消费 sql.ready 事件、内部流转 result.ready。
 * 拓扑常量统一引用 {@link MqConst}，通过 {@link MqTopology} 声明带死信的完整拓扑。
 */
@Configuration
public class RabbitConfig {

    /** NL→SQL 请求交换机（生产）+ 死信拓扑 */
    @Bean
    public Declarables nl2sqlTopology() {
        return MqTopology.topologyWithDlq(
                MqConst.Nl2Sql.EXCHANGE, MqConst.Nl2Sql.QUEUE, MqConst.Nl2Sql.ROUTING_KEY,
                MqConst.Nl2Sql.DLX, MqConst.Nl2Sql.DLQ, MqConst.Nl2Sql.DLK);
    }

    /** SQL 生成完成队列（消费）+ 死信拓扑 */
    @Bean
    public Declarables sqlReadyTopology() {
        return MqTopology.topologyWithDlq(
                MqConst.SqlReady.EXCHANGE, MqConst.SqlReady.QUEUE, MqConst.SqlReady.ROUTING_KEY,
                MqConst.SqlReady.DLX, MqConst.SqlReady.DLQ, MqConst.SqlReady.DLK);
    }

    /** 结果待可视化队列（内部流转）+ 死信拓扑 */
    @Bean
    public Declarables resultReadyTopology() {
        return MqTopology.topologyWithDlq(
                MqConst.ResultReady.EXCHANGE, MqConst.ResultReady.QUEUE, MqConst.ResultReady.ROUTING_KEY,
                MqConst.ResultReady.DLX, MqConst.ResultReady.DLQ, MqConst.ResultReady.DLK);
    }
}
