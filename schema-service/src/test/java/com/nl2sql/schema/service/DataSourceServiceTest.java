package com.nl2sql.schema.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.schema.dto.TableSchemaDTO;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.entity.SchemaCache;
import com.nl2sql.schema.entity.TableListCache;
import com.nl2sql.schema.repository.DataSourceRepository;
import com.nl2sql.schema.repository.SchemaCacheRepository;
import com.nl2sql.schema.repository.TableListCacheRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DataSourceService - 数据源业务逻辑")
@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

    @Mock
    private DataSourceRepository repository;

    @Mock
    private SchemaCacheRepository schemaCacheRepository;

    @Mock
    private TableListCacheRepository tableListCacheRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

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
    @DisplayName("scanTables 从 table_list_cache 反序列化表名")
    void shouldReadTablesFromCache() {
        TableListCache list = new TableListCache();
        list.setTableJson("[\"users\",\"orders\"]");
        when(tableListCacheRepository.findByDataSourceId(1L)).thenReturn(Optional.of(list));

        assertThat(service.scanTables(1L)).containsExactly("users", "orders");
    }

    @Test
    @DisplayName("scanTables 未扫描过返回空列表")
    void shouldReturnEmptyWhenNotScanned() {
        when(tableListCacheRepository.findByDataSourceId(9L)).thenReturn(Optional.empty());

        assertThat(service.scanTables(9L)).isEmpty();
    }

    @Test
    @DisplayName("getTableDetail 从 schema_cache 反序列化组装 DTO（含可空/注释）")
    void shouldAssembleDetailFromCache() {
        SchemaCache cache = new SchemaCache();
        cache.setTableName("users");
        cache.setTableComment("用户表");
        cache.setColumnJson("[{\"name\":\"id\",\"type\":\"bigint\",\"comment\":\"主键\","
                + "\"nullable\":false,\"defaultValue\":null,\"ordinalPosition\":1}]");
        cache.setPrimaryKeyJson("[\"id\"]");
        cache.setIndexJson("[]");
        cache.setForeignKeyJson("[]");
        when(schemaCacheRepository.findByDataSourceIdAndTableName(1L, "users"))
                .thenReturn(Optional.of(cache));

        TableSchemaDTO dto = service.getTableDetail(1L, "users");

        assertThat(dto.getTableName()).isEqualTo("users");
        assertThat(dto.getTableComment()).isEqualTo("用户表");
        assertThat(dto.getColumns()).hasSize(1);
        assertThat(dto.getColumns().get(0).getName()).isEqualTo("id");
        assertThat(dto.getColumns().get(0).isNullable()).isFalse();
        assertThat(dto.getColumns().get(0).getComment()).isEqualTo("主键");
        assertThat(dto.getPrimaryKeys()).containsExactly("id");
    }
}
