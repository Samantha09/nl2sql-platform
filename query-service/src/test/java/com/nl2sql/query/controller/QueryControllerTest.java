package com.nl2sql.query.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.common.PageResult;
import com.nl2sql.query.dto.QueryRequest;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.entity.QueryHistory;
import com.nl2sql.query.service.QueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("QueryController - 查询接口切片测试")
@WebMvcTest(QueryController.class)
@ActiveProfiles("test")
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QueryService queryService;

    @Test
    @DisplayName("POST /api/query/nl 应返回 NL 查询结果")
    void shouldQueryByNaturalLanguage() throws Exception {
        QueryRequest request = new QueryRequest();
        request.setUserId(1L);
        request.setDataSourceId(1L);
        request.setNaturalLanguage("本月销售额");

        QueryResult result = new QueryResult();
        result.setSql("SELECT * FROM orders LIMIT 100;");
        result.setData(List.of(Map.of("id", 1)));
        result.setTotalCount(1);

        when(queryService.queryByNaturalLanguage(any(QueryRequest.class))).thenReturn(result);

        mockMvc.perform(post("/api/query/nl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sql").value("SELECT * FROM orders LIMIT 100;"));
    }

    @Test
    @DisplayName("POST /api/query/sql 应返回 SQL 执行结果")
    void shouldQueryBySql() throws Exception {
        QueryResult result = new QueryResult();
        result.setSql("SELECT 1");
        result.setTotalCount(1);

        when(queryService.queryBySql("SELECT 1")).thenReturn(result);

        mockMvc.perform(post("/api/query/sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("SELECT 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sql").value("SELECT 1"));
    }

    @Test
    @DisplayName("GET /api/query/history 应返回历史列表")
    void shouldListHistory() throws Exception {
        QueryHistory history = new QueryHistory();
        history.setId(1L);
        history.setNaturalLanguage("本月销售额");

        when(queryService.history("conv-1")).thenReturn(List.of(history));

        mockMvc.perform(get("/api/query/history")
                        .param("conversationId", "conv-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].naturalLanguage").value("本月销售额"));
    }

    @Test
    @DisplayName("GET /api/query/history/page 应返回分页历史")
    void shouldPageHistory() throws Exception {
        PageResult<QueryHistory> pageResult = PageResult.of(List.of(), 0, 1, 10);

        when(queryService.historyPage("conv-1", 1, 10)).thenReturn(pageResult);

        mockMvc.perform(get("/api/query/history/page")
                        .param("conversationId", "conv-1")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
