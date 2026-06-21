package com.nl2sql.schema.service;

import com.nl2sql.schema.dto.TableSchemaDTO;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.repository.DataSourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DataSourceService - 数据源业务逻辑")
@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

    @Mock
    private DataSourceRepository repository;

    @InjectMocks
    private DataSourceService service;

    @Test
    @DisplayName("create 应调用 repository.save")
    void shouldSaveWhenCreate() {
        DataSourceConfig config = new DataSourceConfig();
        config.setName("订单库");
        when(repository.save(config)).thenReturn(config);

        DataSourceConfig result = service.create(config);

        assertThat(result.getName()).isEqualTo("订单库");
        verify(repository).save(config);
    }

    @Test
    @DisplayName("list 应返回 repository.findAll 的结果")
    void shouldListAll() {
        DataSourceConfig config = new DataSourceConfig();
        config.setName("订单库");
        when(repository.findAll()).thenReturn(List.of(config));

        List<DataSourceConfig> result = service.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("订单库");
    }

    @Test
    @DisplayName("delete 应调用 repository.deleteById")
    void shouldDeleteById() {
        service.delete(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("scanTables 应返回固定 Mock 表名列表")
    void shouldReturnMockTables() {
        List<String> tables = service.scanTables(1L);
        assertThat(tables).containsExactly("users", "orders", "products");
    }

    @Test
    @DisplayName("getTableDetail 应返回带字段信息的 Mock DTO")
    void shouldReturnMockTableDetail() {
        TableSchemaDTO dto = service.getTableDetail(1L, "users");

        assertThat(dto.getTableName()).isEqualTo("users");
        assertThat(dto.getColumns()).hasSize(2);
        assertThat(dto.getPrimaryKeys()).containsExactly("id");
    }
}
