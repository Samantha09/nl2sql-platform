package com.nl2sql.ai.listener;

import com.nl2sql.ai.config.RabbitConfig;
import com.nl2sql.ai.service.MockLLMService;
import com.nl2sql.common.event.NL2SQLEvent;
import com.nl2sql.common.event.SQLReadyEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NL2SQLListener {

    private final MockLLMService mockLLMService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitConfig.NL2SQL_QUEUE)
    public void onNL2SQLEvent(NL2SQLEvent event) {
        String sql = mockLLMService.convert(event.getNaturalLanguage(), event.getDataSourceId());

        SQLReadyEvent ready = new SQLReadyEvent();
        ready.setEventId(UUID.randomUUID().toString());
        ready.setNlRequestId(event.getEventId());
        ready.setGeneratedSql(sql);
        ready.setSqlValid(true);
        ready.setTimestamp(System.currentTimeMillis());

        rabbitTemplate.convertAndSend("sql.ready.exchange", "sql.ready.event", ready);
    }
}
