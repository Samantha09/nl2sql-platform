package com.nl2sql.common.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.aopalliance.intercept.MethodInterceptor;

/**
 * RabbitMQ 自动装配：在 Spring Boot 默认 AMQP 之上增强可靠性。
 * <ul>
 *   <li>消息体以 JSON 传输（替代 JDK 序列化），跨语言可读</li>
 *   <li>publisher-confirms / returns 回调，发送失败可观测</li>
 *   <li>消费端有限次重试，超限通过 {@link RepublishMessageRecoverer} 投递到死信队列</li>
 * </ul>
 * 仅在 classpath 存在 AMQP 时生效，对未引入 RabbitMQ 的服务无副作用。
 */
@AutoConfiguration
@ConditionalOnClass({RabbitTemplate.class, ConnectionFactory.class})
@EnableConfigurationProperties(MqProperties.class)
public class RabbitJsonConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitJsonConfig.class);

    /** JSON 消息转换器：事件对象 ↔ JSON */
    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** 生产端 RabbitTemplate：JSON 转换 + 发送确认/退回回调 */
    @Bean
    @ConditionalOnMissingBean(RabbitTemplate.class)
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        template.setConfirmCallback((correlation, ack, cause) -> {
            if (!ack) {
                log.error("[MQ] 消息未到达交换机, cause={}, correlation={}", cause, correlation);
            }
        });
        template.setReturnsCallback(returned ->
                log.error("[MQ] 消息无法路由到队列, exchange={}, routingKey={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        return template;
    }

    /**
     * 消费端容器工厂：JSON 转换 + 有限重试 + 超限投递死信。
     * <p>重试在内存中进行，耗尽 {@code maxRetries} 后由 RepublishMessageRecoverer
     * 把消息发到原队列绑定的死信交换机，避免毒消息无限重投阻塞队列。
     */
    @Bean
    @ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter,
            RabbitTemplate rabbitTemplate, MqProperties props) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);

        // 有限重试 + 指数退避；耗尽后由 RepublishMessageRecoverer 投递死信
        MethodInterceptor interceptor = RetryInterceptorBuilder.stateless()
                .maxAttempts(Math.max(1, props.getMaxRetries()))
                .backOffOptions(props.getInitialInterval(), props.getMultiplier(), props.getMaxInterval())
                .recoverer(new RepublishMessageRecoverer(rabbitTemplate))
                .build();
        factory.setAdviceChain(interceptor);
        return factory;
    }
}
