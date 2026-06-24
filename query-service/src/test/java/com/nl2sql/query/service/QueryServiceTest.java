package com.nl2sql.query.service;

import com.nl2sql.common.PageResult;
import com.nl2sql.common.R;
import com.nl2sql.common.dto.ConvertRequest;
import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.feign.AiServiceClient;
import com.nl2sql.common.feign.SchemaServiceClient;
import com.nl2sql.query.dto.QueryRequest;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.entity.QueryHistory;
import com.nl2sql.query.executor.SqlExecutor;
import com.nl2sql.query.repository.QueryHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("QueryService - 真实同步链路")
@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock private AiServiceClient aiServiceClient;
    @Mock private SchemaServiceClient schemaServiceClient;
    @Mock private SqlExecutor sqlExecutor;
    @Mock private QueryHistoryRepository historyRepository;
    @InjectMocks private QueryService queryService;

    @Test
    @DisplayName("queryByNaturalLanguage 应走 convert→execute→存历史")
    void shouldRunRealLink() {
        QueryRequest request = new QueryRequest();
        request.setDataSourceId(1L);
        request.setNaturalLanguage("本月销售额");

        DataSourceConnectionDTO conn = new DataSourceConnectionDTO();
        conn.setType("mysql");
        conn.setDatabaseNames(List.of("shop"));
        when(schemaServiceClient.getConnection(1L)).thenReturn(R.ok(conn));
        when(aiServiceClient.convert(any(ConvertRequest.class))).thenReturn(R.ok("SELECT * FROM orders;"));

        QueryResult exec = new QueryResult();
        exec.setSql("SELECT * FROM orders;");
        exec.setData(List.of(Map.of("id", 1)));
        exec.setTotalCount(1);
        exec.setChartType("table");
        exec.setExecuteTimeMs(10L);
        when(sqlExecutor.execute(conn, "shop", "SELECT * FROM orders;")).thenReturn(exec);

        QueryResult result = queryService.queryByNaturalLanguage(request);

        assertThat(result.getSql()).isEqualTo("SELECT * FROM orders;");
        ArgumentCaptor<ConvertRequest> captor = ArgumentCaptor.forClass(ConvertRequest.class);
        verify(aiServiceClient).convert(captor.capture());
        assertThat(captor.getValue().getDatabaseName()).isEqualTo("shop");
        verify(historyRepository).save(any(QueryHistory.class));
    }

    @Test
    @DisplayName("queryBySql 保持原有 mock 行为")
    void shouldReturnMockForSql() {
        QueryResult result = queryService.queryBySql("SELECT 1");
        assertThat(result.getSql()).isEqualTo("SELECT 1");
        assertThat(result.getData()).containsExactly(Map.of("result", "mock"));
    }

    @Test
    @DisplayName("history 应按 conversationId 查询")
    void shouldFindHistory() {
        QueryHistory h = new QueryHistory();
        h.setConversationId("conv-1");
        when(historyRepository.findByConversationIdOrderByCreatedAtDesc("conv-1")).thenReturn(List.of(h));
        assertThat(queryService.history("conv-1")).hasSize(1);
    }

    @Test
    @DisplayName("historyPage 应返回分页结构")
    void shouldReturnHistoryPage() {
        QueryHistory h = new QueryHistory();
        h.setId(1L);
        when(historyRepository.findByConversationIdOrderByCreatedAtDesc(eq("conv-1"), any()))
                .thenReturn(new PageImpl<>(List.of(h), PageRequest.of(0, 10), 1));
        PageResult<QueryHistory> p = queryService.historyPage("conv-1", 1, 10);
        assertThat(p.getTotal()).isEqualTo(1);
        assertThat(p.getRecords()).hasSize(1);
    }
}
