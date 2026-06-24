package com.nl2sql.schema.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.dto.SchemaContextDTO;
import com.nl2sql.common.exception.BaseException;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.entity.SchemaCache;
import com.nl2sql.schema.exception.SchemaResultCode;
import com.nl2sql.schema.repository.DataSourceRepository;
import com.nl2sql.schema.repository.SchemaCacheRepository;
import com.nl2sql.schema.repository.TableListCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("DataSourceService - 内部接口（连接信息/精简 schema）")
@ExtendWith(MockitoExtension.class)
class DataSourceServiceInternalTest {

    @Mock private DataSourceRepository repository;
    @Mock private SchemaCacheRepository schemaCacheRepository;
    @Mock private TableListCacheRepository tableListCacheRepository;
    private DataSourceService service;

    @BeforeEach
    void setUp() {
        // 用真实 ObjectMapper 反序列化 schema_cache 的 columnJson，避免 mock TypeReference 的引用相等匹配脆弱
        service = new DataSourceService(repository, schemaCacheRepository, tableListCacheRepository, new ObjectMapper());
    }

    private DataSourceConfig ds(long id) {
        DataSourceConfig d = new DataSourceConfig();
        d.setId(id);
        d.setType("mysql");
        d.setHost("h");
        d.setPort(3306);
        d.setDatabaseNames(List.of("shop"));
        d.setUsername("u");
        d.setPasswordEncrypted("plainpwd"); // 非 ENC(...)，直接当明文
        return d;
    }

    @Test
    @DisplayName("getConnection 应返回解密后的连接信息（非 ENC 密文按明文返回）")
    void shouldReturnConnectionWithPlaintextPassword() {
        when(repository.findById(1L)).thenReturn(Optional.of(ds(1L)));
        DataSourceConnectionDTO conn = service.getConnection(1L);
        assertThat(conn.getHost()).isEqualTo("h");
        assertThat(conn.getType()).isEqualTo("mysql");
        assertThat(conn.getDatabaseNames()).containsExactly("shop");
        assertThat(conn.getPassword()).isEqualTo("plainpwd");
    }

    @Test
    @DisplayName("getConnection 数据源不存在应抛 DATASOURCE_NOT_FOUND")
    void shouldThrowWhenDatasourceMissing() {
        when(repository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getConnection(2L))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(SchemaResultCode.DATASOURCE_NOT_FOUND.getDesc());
    }

    @Test
    @DisplayName("getSchemaContext 应把 schema_cache 组装为精简 schema")
    void shouldBuildContextFromCache() {
        SchemaCache c = new SchemaCache();
        c.setTableName("orders");
        c.setTableComment("订单表");
        c.setColumnJson("[{\"name\":\"id\",\"type\":\"bigint\",\"comment\":\"主键\"}]");
        when(schemaCacheRepository.findByDataSourceIdAndDatabaseName(1L, "shop"))
                .thenReturn(List.of(c));

        SchemaContextDTO ctx = service.getSchemaContext(1L, "shop");
        assertThat(ctx.getDatabase()).isEqualTo("shop");
        assertThat(ctx.getTables()).hasSize(1);
        assertThat(ctx.getTables().get(0).getTableName()).isEqualTo("orders");
        assertThat(ctx.getTables().get(0).getColumns().get(0).getName()).isEqualTo("id");
    }
}
