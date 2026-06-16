package com.nl2sql.query.service;

import com.nl2sql.common.event.NL2SQLEvent;
import com.nl2sql.query.config.RabbitConfig;
import com.nl2sql.query.dto.QueryRequest;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.entity.QueryHistory;
import com.nl2sql.query.repository.QueryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class QueryService {

    private final RabbitTemplate rabbitTemplate;
    private final QueryHistoryRepository historyRepository;

    public QueryResult queryByNaturalLanguage(QueryRequest request) {
        NL2SQLEvent event = new NL2SQLEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setUserId(request.getUserId());
        event.setDataSourceId(request.getDataSourceId());
        event.setNaturalLanguage(request.getNaturalLanguage());
        event.setConversationId(request.getConversationId());
        event.setTimestamp(System.currentTimeMillis());

        rabbitTemplate.convertAndSend(RabbitConfig.NL2SQL_EXCHANGE, RabbitConfig.NL2SQL_ROUTING_KEY, event);

        // 骨架阶段：同步返回 Mock 结果
        QueryResult result = new QueryResult();
        result.setSql("SELECT * FROM orders LIMIT 100;");
        result.setData(List.of(
            Map.of("id", 1, "product_name", "华为Mate60", "amount", 1234567),
            Map.of("id", 2, "product_name", "iPhone15", "amount", 987654)
        ));
        result.setTotalCount(2);
        result.setExecuteTimeMs(120L);
        result.setChartType("table");

        saveHistory(request, result, null);
        return result;
    }

    public QueryResult queryBySql(String sql) {
        QueryResult result = new QueryResult();
        result.setSql(sql);
        result.setData(List.of(Map.of("result", "mock")));
        result.setTotalCount(1);
        result.setExecuteTimeMs(50L);
        result.setChartType("table");
        return result;
    }

    public List<QueryHistory> history(String conversationId) {
        return historyRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
    }

    private void saveHistory(QueryRequest request, QueryResult result, String error) {
        QueryHistory h = new QueryHistory();
        h.setConversationId(request.getConversationId());
        h.setUserId(request.getUserId());
        h.setDataSourceId(request.getDataSourceId());
        h.setNaturalLanguage(request.getNaturalLanguage());
        h.setGeneratedSql(result.getSql());
        h.setSqlExecuted(result.getSql());
        h.setExecuteTimeMs(result.getExecuteTimeMs());
        h.setResultCount(result.getTotalCount());
        h.setStatus(error == null ? "success" : "failed");
        h.setErrorMessage(error);
        h.setChartType(result.getChartType());
        historyRepository.save(h);
    }
}
