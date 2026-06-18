package com.nl2sql.common.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 声明式拓扑工具：一次性声明「业务队列 + 绑定 + 死信交换机 + 死信队列」的完整组合，
 * 把样板代码从各服务 RabbitConfig 收口到此处。
 * <p>用法：在服务的 {@code @Configuration} 中返回 {@code topologyWithDlq(...)}，
 * Spring AMQP 会自动声明返回的全部 {@link Declarable}。
 */
public final class MqTopology {

    private MqTopology() {
    }

    /**
     * 声明一组带死信的拓扑：
     * <ul>
     *   <li>业务 DirectExchange + Queue + Binding，队列绑定死信交换机</li>
     *   <li>死信 DirectExchange + Queue + Binding</li>
     * </ul>
     *
     * @param exchange   业务交换机名
     * @param queue      业务队列名
     * @param routingKey 业务路由键
     * @param dlx        死信交换机名
     * @param dlq        死信队列名
     * @param dlk        死信路由键
     */
    public static Declarables topologyWithDlq(String exchange, String queue, String routingKey,
                                              String dlx, String dlq, String dlk) {
        List<Declarable> declarables = new ArrayList<>();

        // 业务交换机 + 队列（绑定死信交换机）+ 绑定
        DirectExchange bizExchange = new DirectExchange(exchange, true, false);
        Queue bizQueue = QueueBuilder.durable(queue)
                .withArgument("x-dead-letter-exchange", dlx)
                .withArgument("x-dead-letter-routing-key", dlk)
                .build();
        declarables.add(bizExchange);
        declarables.add(bizQueue);
        declarables.add(BindingBuilder.bind(bizQueue).to(bizExchange).with(routingKey));

        // 死信交换机 + 死信队列 + 绑定
        DirectExchange deadExchange = new DirectExchange(dlx, true, false);
        Queue deadQueue = QueueBuilder.durable(dlq).build();
        declarables.add(deadExchange);
        declarables.add(deadQueue);
        declarables.add(BindingBuilder.bind(deadQueue).to(deadExchange).with(dlk));

        return new Declarables(declarables);
    }

    /** 仅声明业务交换机（供仅生产、不消费的服务使用，如 query→ai 只发不收 nl2sql 队列） */
    public static Binding bind(Queue queue, DirectExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
