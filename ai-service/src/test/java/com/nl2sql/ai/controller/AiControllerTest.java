package com.nl2sql.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.ai.service.Nl2SqlConvertService;
import com.nl2sql.common.dto.ConvertRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AiController - AI 接口切片测试")
@WebMvcTest(AiController.class)
@ActiveProfiles("test")
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Nl2SqlConvertService convertService;

    @Test
    @DisplayName("POST /api/ai/convert 应返回转换后的 SQL")
    void shouldConvertNaturalLanguage() throws Exception {
        ConvertRequest request = new ConvertRequest();
        request.setDataSourceId(1L);
        request.setNaturalLanguage("本月销售额");

        when(convertService.convert(any(ConvertRequest.class)))
                .thenReturn("SELECT product_name, SUM(amount) FROM orders GROUP BY product_name");

        mockMvc.perform(post("/api/ai/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data")
                        .value("SELECT product_name, SUM(amount) FROM orders GROUP BY product_name"));
    }

    @Test
    @DisplayName("POST /api/ai/validate 应校验 SQL 是否以 SELECT 开头")
    void shouldValidateSelectSql() throws Exception {
        mockMvc.perform(post("/api/ai/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("SELECT * FROM users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("POST /api/ai/validate 对非 SELECT 语句应返回 false")
    void shouldInvalidateNonSelectSql() throws Exception {
        mockMvc.perform(post("/api/ai/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("DELETE FROM users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));
    }
}
