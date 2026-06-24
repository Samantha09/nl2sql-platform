package com.nl2sql.schema.controller;

import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.dto.SchemaContextDTO;
import com.nl2sql.schema.service.DataSourceService;
import com.nl2sql.schema.service.SchemaScanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DataSourceController - 内部接口")
@WebMvcTest(DataSourceController.class)
@ActiveProfiles("test")
class DataSourceInternalControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DataSourceService service;
    @MockBean private SchemaScanService scanService;

    @Test
    @DisplayName("GET /api/schema/internal/datasource/{id}/connection 应返回连接信息")
    void shouldReturnConnection() throws Exception {
        DataSourceConnectionDTO dto = new DataSourceConnectionDTO();
        dto.setHost("h"); dto.setPort(3306); dto.setType("mysql");
        when(service.getConnection(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/schema/internal/datasource/1/connection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.host").value("h"));
    }

    @Test
    @DisplayName("GET /api/schema/internal/datasource/{id}/schema 应返回精简 schema")
    void shouldReturnSchema() throws Exception {
        SchemaContextDTO ctx = new SchemaContextDTO();
        ctx.setDatabase("shop");
        ctx.setTables(List.of());
        when(service.getSchemaContext(1L, "shop")).thenReturn(ctx);

        mockMvc.perform(get("/api/schema/internal/datasource/1/schema").param("database", "shop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.database").value("shop"));
    }
}
