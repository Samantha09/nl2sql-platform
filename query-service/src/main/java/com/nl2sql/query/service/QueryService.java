package com.nl2sql.query.service;

import com.nl2sql.common.cache.CacheNames;
import com.nl2sql.common.PageResult;
import com.nl2sql.common.event.NL2SQLEvent;
import com.nl2sql.common.mq.MqConst;
import com.nl2sql.query.dto.QueryRequest;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.entity.QueryHistory;
import com.nl2sql.query.repository.QueryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

        rabbitTemplate.convertAndSend(MqConst.Nl2Sql.EXCHANGE, MqConst.Nl2Sql.ROUTING_KEY, event);

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

    /** 按会话查历史，结果缓存到 Redis；空会话不缓存避免无意义条目 */
    @Cacheable(cacheNames = CacheNames.QUERY_HISTORY, key = "#conversationId", unless = "#conversationId == null")
    public List<QueryHistory> history(String conversationId) {
        return historyRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
    }

    /** 按会话分页查历史（不缓存：分页参数组合多，缓存命中率低） */
    public PageResult<QueryHistory> historyPage(String conversationId, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(pageNum - 1, 0), pageSize);
        Page<QueryHistory> page = historyRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        return PageResult.of(page.getContent(), page.getTotalElements(), pageNum, pageSize);
    }

    /** 写入历史后清除该会话缓存，保证下次读取最新 */
    @CacheEvict(cacheNames = CacheNames.QUERY_HISTORY, key = "#request.conversationId",
            condition = "#request.conversationId != null")
    void saveHistory(QueryRequest request, QueryResult result, String error) {
        QueryHistory h = new QueryHistory();
        // 非空字段兜底：匿名/无会话/未指定数据源时填默认值，避免违反非空约束
        h.setConversationId(request.getConversationId() != null
                ? request.getConversationId() : UUID.randomUUID().toString());
        h.setUserId(request.getUserId() != null ? request.getUserId() : 0L);
        h.setDataSourceId(request.getDataSourceId() != null ? request.getDataSourceId() : 0L);
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
