package com.nl2sql.query.service;

import com.nl2sql.common.PageResult;
import com.nl2sql.common.cache.CacheNames;
import com.nl2sql.common.dto.ConvertRequest;
import com.nl2sql.common.dto.ConvertResponse;
import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.enums.ResultCode;
import com.nl2sql.common.exception.BaseException;
import com.nl2sql.common.feign.AiServiceClient;
import com.nl2sql.common.feign.SchemaServiceClient;
import com.nl2sql.query.dto.QueryRequest;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.entity.QueryHistory;
import com.nl2sql.query.executor.SqlExecutor;
import com.nl2sql.query.exception.QueryResultCode;
import com.nl2sql.query.repository.QueryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueryService {

    private final AiServiceClient aiServiceClient;
    private final SchemaServiceClient schemaServiceClient;
    private final SqlExecutor sqlExecutor;
    private final QueryHistoryRepository historyRepository;

    /** 同步真实链路：取连接 → 确定 database → 意图识别 → 生成/回复 → 执行 → 存历史。 */
    public QueryResult queryByNaturalLanguage(QueryRequest request) {
        DataSourceConnectionDTO conn = schemaServiceClient
                .getConnection(request.getDataSourceId()).getData();
        String database = resolveDatabase(request, conn);

        ConvertRequest convertReq = new ConvertRequest();
        convertReq.setDataSourceId(request.getDataSourceId());
        convertReq.setNaturalLanguage(request.getNaturalLanguage());
        convertReq.setDatabaseName(database);
        ConvertResponse convertResp = aiServiceClient.convert(convertReq).getData();

        QueryResult result;
        if ("sql".equalsIgnoreCase(convertResp.getType())) {
            result = sqlExecutor.execute(conn, database, convertResp.getContent());
            result.setType("sql");
        } else {
            // chat / clarification 等意图：不执行 SQL，直接返回文本回复
            result = new QueryResult();
            result.setType(convertResp.getType());
            result.setSql(convertResp.getContent());
            result.setData(List.of());
            result.setTotalCount(0);
            result.setExecuteTimeMs(0L);
            result.setChartType("table");
        }
        saveHistory(request, result, convertResp.getType());
        return result;
    }

    /** 解析目标库：显式指定优先；否则数据源唯一库自动选用；多库报错。 */
    private String resolveDatabase(QueryRequest request, DataSourceConnectionDTO conn) {
        if (request.getDatabaseName() != null && !request.getDatabaseName().isBlank()) {
            return request.getDatabaseName();
        }
        List<String> dbs = conn.getDatabaseNames();
        if (dbs == null || dbs.isEmpty()) {
            throw new BaseException(ResultCode.BAD_REQUEST, "数据源未配置任何库");
        }
        if (dbs.size() == 1) {
            return dbs.get(0);
        }
        throw new BaseException(QueryResultCode.QUERY_DATABASE_NOT_SPECIFIED);
    }

    /** 直接执行 SQL（本轮保留原行为，未接真实执行——后续可扩展）。 */
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
    void saveHistory(QueryRequest request, QueryResult result, String intentType) {
        QueryHistory h = new QueryHistory();
        h.setConversationId(request.getConversationId() != null
                ? request.getConversationId() : UUID.randomUUID().toString());
        h.setUserId(request.getUserId() != null ? request.getUserId() : 0L);
        h.setDataSourceId(request.getDataSourceId() != null ? request.getDataSourceId() : 0L);
        h.setNaturalLanguage(request.getNaturalLanguage());
        h.setGeneratedSql(result.getSql());
        h.setSqlExecuted(result.getSql());
        h.setExecuteTimeMs(result.getExecuteTimeMs());
        h.setResultCount(result.getTotalCount());
        h.setStatus(intentType);
        h.setChartType(result.getChartType());
        historyRepository.save(h);
    }
}
