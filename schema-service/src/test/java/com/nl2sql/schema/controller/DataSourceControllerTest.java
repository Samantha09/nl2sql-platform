package com.nl2sql.schema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.service.DataSourceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DataSourceController - 数据源接口切片测试")
@WebMvcTest(DataSourceController.class)
@ActiveProfiles("test")
class DataSourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataSourceService dataSourceService;

    @Test
    @DisplayName("POST /api/schema/datasource 应返回创建后的数据源")
    void shouldCreateDataSource() throws Exception {
        DataSourceConfig config = new DataSourceConfig();
        config.setId(1L);
        config.setName("订单库");
        config.setType("mysql");

        when(dataSourceService.create(any(DataSourceConfig.class))).thenReturn(config);

        mockMvc.perform(post("/api/schema/datasource")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("订单库"));
    }

    @Test
    @DisplayName("GET /api/schema/datasource/list 应返回数据源列表")
    void shouldListDataSources() throws Exception {
        DataSourceConfig config = new DataSourceConfig();
        config.setId(1L);
        config.setName("订单库");

        when(dataSourceService.list()).thenReturn(List.of(config));

        mockMvc.perform(get("/api/schema/datasource/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("订单库"));
    }

    @Test
    @DisplayName("DELETE /api/schema/datasource/{id} 应返回成功响应")
    void shouldDeleteDataSource() throws Exception {
        mockMvc.perform(delete("/api/schema/datasource/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
    }
}
