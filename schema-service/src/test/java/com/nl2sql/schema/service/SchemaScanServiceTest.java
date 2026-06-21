package com.nl2sql.schema.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.entity.SchemaCache;
import com.nl2sql.schema.entity.TableListCache;
import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.repository.DataSourceRepository;
import com.nl2sql.schema.repository.SchemaCacheRepository;
import com.nl2sql.schema.repository.TableListCacheRepository;
import com.nl2sql.schema.scanner.DatabaseScanner;
import com.nl2sql.schema.scanner.ScannerRegistry;
import com.nl2sql.schema.scanner.model.SchemaMetadata;
import com.nl2sql.schema.scanner.model.TableMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SchemaScanService - 扫描编排")
@ExtendWith(MockitoExtension.class)
class SchemaScanServiceTest {

    @Mock DataSourceRepository dataSourceRepository;
    @Mock SchemaCacheRepository schemaCacheRepository;
    @Mock TableListCacheRepository tableListCacheRepository;
    @Mock ScannerRegistry scannerRegistry;
    @Mock DatabaseScanner scanner;

    SchemaScanService service;

    @BeforeEach
    void setUp() {
        service = new SchemaScanService(dataSourceRepository, schemaCacheRepository,
                tableListCacheRepository, scannerRegistry, new ObjectMapper());
    }

    private DataSourceConfig ds() {
        DataSourceConfig d = new DataSourceConfig();
        d.setId(1L);
        d.setType("mysql");
        d.setHost("localhost");
        d.setPort(3306);
        d.setDatabaseName("shop");
        d.setUsername("u");
        d.setPasswordEncrypted("plainpwd"); // 非 ENC()，按明文处理，避免依赖环境密钥
        return d;
    }

    private SchemaMetadata oneTable() {
        TableMetadata t = new TableMetadata();
        t.setName("users");
        t.setComment("用户表");
        SchemaMetadata m = new SchemaMetadata();
        m.getTables().add(t);
        return m;
    }

    @Test
    @DisplayName("scan 解析类型→调度 scanner→持久化 schema_cache 与 table_list_cache，返回表名")
    void shouldScanAndPersist() {
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.of(ds()));
        when(scannerRegistry.resolve(DbType.MYSQL)).thenReturn(scanner);
        when(scanner.scan(any())).thenReturn(oneTable());
        when(schemaCacheRepository.findByDataSourceIdAndTableName(1L, "users"))
                .thenReturn(Optional.empty());
        when(schemaCacheRepository.findByDataSourceId(1L)).thenReturn(List.of());

        List<String> tables = service.scan(1L);

        assertThat(tables).containsExactly("users");
        ArgumentCaptor<SchemaCache> cap = ArgumentCaptor.forClass(SchemaCache.class);
        verify(schemaCacheRepository).save(cap.capture());
        assertThat(cap.getValue().getTableName()).isEqualTo("users");
        assertThat(cap.getValue().getVersion()).isEqualTo(1);
        verify(tableListCacheRepository).save(any(TableListCache.class));
    }

    @Test
    @DisplayName("重扫描已存在的表 version 自增")
    void shouldBumpVersionOnRescan() {
        SchemaCache existing = new SchemaCache();
        existing.setDataSourceId(1L);
        existing.setTableName("users");
        existing.setVersion(3);
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.of(ds()));
        when(scannerRegistry.resolve(DbType.MYSQL)).thenReturn(scanner);
        when(scanner.scan(any())).thenReturn(oneTable());
        when(schemaCacheRepository.findByDataSourceIdAndTableName(1L, "users"))
                .thenReturn(Optional.of(existing));
        when(schemaCacheRepository.findByDataSourceId(1L)).thenReturn(List.of(existing));

        service.scan(1L);

        assertThat(existing.getVersion()).isEqualTo(4);
    }
}
