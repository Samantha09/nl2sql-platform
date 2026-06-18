package com.nl2sql.query.listener;

import com.nl2sql.common.event.SQLReadyEvent;
import com.nl2sql.common.mq.MqConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SQLReadyListener {

    @RabbitListener(queues = MqConst.SqlReady.QUEUE)
    public void onSQLReady(SQLReadyEvent event) {
        log.info("收到 SQL 生成完成事件: eventId={}, sql={}", event.getEventId(), event.getGeneratedSql());
        // 骨架阶段仅打印日志，真实执行后续实现
    }
}
