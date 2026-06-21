package com.nl2sql.query.service;

import com.nl2sql.common.PageResult;
import com.nl2sql.common.mq.MqConst;
import com.nl2sql.query.dto.QueryRequest;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.entity.QueryHistory;
import com.nl2sql.query.repository.QueryHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("QueryService - 查询业务逻辑")
@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private QueryHistoryRepository historyRepository;

    @InjectMocks
    private QueryService queryService;

    @Test
    @DisplayName("queryByNaturalLanguage 应发送 NL2SQLEvent 并保存历史")
    void shouldSendEventAndSaveHistory() {
        QueryRequest request = new QueryRequest();
        request.setUserId(1L);
        request.setDataSourceId(1L);
        request.setNaturalLanguage("本月销售额");
        request.setConversationId("conv-1");

        QueryResult result = queryService.queryByNaturalLanguage(request);

        assertThat(result.getSql()).isEqualTo("SELECT * FROM orders LIMIT 100;");
        assertThat(result.getTotalCount()).isEqualTo(2);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq(MqConst.Nl2Sql.EXCHANGE), eq(MqConst.Nl2Sql.ROUTING_KEY), eventCaptor.capture());
        assertThat(eventCaptor.getValue()).hasFieldOrPropertyWithValue("naturalLanguage", "本月销售额");

        verify(historyRepository).save(any(QueryHistory.class));
    }

    @Test
    @DisplayName("queryBySql 应直接返回 Mock 结果")
    void shouldReturnMockForSql() {
        QueryResult result = queryService.queryBySql("SELECT 1");
        assertThat(result.getSql()).isEqualTo("SELECT 1");
        assertThat(result.getData()).containsExactly(Map.of("result", "mock"));
    }

    @Test
    @DisplayName("history 应调用 repository 按 conversationId 查询")
    void shouldFindHistoryByConversationId() {
        QueryHistory history = new QueryHistory();
        history.setConversationId("conv-1");
        when(historyRepository.findByConversationIdOrderByCreatedAtDesc("conv-1"))
                .thenReturn(List.of(history));

        List<QueryHistory> result = queryService.history("conv-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getConversationId()).isEqualTo("conv-1");
    }

    @Test
    @DisplayName("historyPage 应返回正确分页结构")
    void shouldReturnHistoryPage() {
        QueryHistory history = new QueryHistory();
        history.setId(1L);
        when(historyRepository.findByConversationIdOrderByCreatedAtDesc(eq("conv-1"), any()))
                .thenReturn(new PageImpl<>(List.of(history), PageRequest.of(0, 10), 1));

        PageResult<QueryHistory> result = queryService.historyPage("conv-1", 1, 10);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getRecords()).hasSize(1);
    }
}
