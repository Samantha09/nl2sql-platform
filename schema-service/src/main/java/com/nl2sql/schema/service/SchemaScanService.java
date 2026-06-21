package com.nl2sql.schema.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.common.encrypt.SecureConfigEncryptor;
import com.nl2sql.common.enums.IEnum;
import com.nl2sql.common.exception.BaseException;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.entity.SchemaCache;
import com.nl2sql.schema.entity.TableListCache;
import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.exception.SchemaResultCode;
import com.nl2sql.schema.repository.DataSourceRepository;
import com.nl2sql.schema.repository.SchemaCacheRepository;
import com.nl2sql.schema.repository.TableListCacheRepository;
import com.nl2sql.schema.scanner.ScanContext;
import com.nl2sql.schema.scanner.ScannerRegistry;
import com.nl2sql.schema.scanner.model.SchemaMetadata;
import com.nl2sql.schema.scanner.model.TableMetadata;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 扫描编排：解密连接信息 → 调度 scanner → 覆盖式持久化。 */
@Service
public class SchemaScanService {

    private final DataSourceRepository dataSourceRepository;
    private final SchemaCacheRepository schemaCacheRepository;
    private final TableListCacheRepository tableListCacheRepository;
    private final ScannerRegistry scannerRegistry;
    private final ObjectMapper objectMapper;

    public SchemaScanService(DataSourceRepository dataSourceRepository,
                             SchemaCacheRepository schemaCacheRepository,
                             TableListCacheRepository tableListCacheRepository,
                             ScannerRegistry scannerRegistry,
                             ObjectMapper objectMapper) {
        this.dataSourceRepository = dataSourceRepository;
        this.schemaCacheRepository = schemaCacheRepository;
        this.tableListCacheRepository = tableListCacheRepository;
        this.scannerRegistry = scannerRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<String> scan(Long dataSourceId) {
        DataSourceConfig ds = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new BaseException(SchemaResultCode.DATASOURCE_NOT_FOUND));

        DbType type = IEnum.of(DbType.class, ds.getType())
                .orElseThrow(() -> new BaseException(SchemaResultCode.DB_TYPE_UNSUPPORTED));

        ScanContext ctx = new ScanContext(type, ds.getHost(), ds.getPort(),
                ds.getDatabaseName(), ds.getUsername(), decrypt(ds.getPasswordEncrypted()));

        SchemaMetadata meta = scannerRegistry.resolve(type).scan(ctx);

        persist(dataSourceId, meta);

        return meta.getTables().stream().map(TableMetadata::getName).toList();
    }

    private void persist(Long dataSourceId, SchemaMetadata meta) {
        Set<String> scanned = meta.getTables().stream()
                .map(TableMetadata::getName).collect(Collectors.toSet());
        LocalDateTime now = LocalDateTime.now();

        // 1. upsert 每张表
        for (TableMetadata t : meta.getTables()) {
            SchemaCache cache = schemaCacheRepository
                    .findByDataSourceIdAndTableName(dataSourceId, t.getName())
                    .orElseGet(() -> {
                        SchemaCache c = new SchemaCache();
                        c.setDataSourceId(dataSourceId);
                        c.setTableName(t.getName());
                        c.setVersion(0);
                        return c;
                    });
            cache.setTableComment(t.getComment());
            cache.setColumnJson(toJson(t.getColumns()));
            cache.setPrimaryKeyJson(toJson(t.getPrimaryKeys()));
            cache.setForeignKeyJson(toJson(t.getForeignKeys()));
            cache.setIndexJson(toJson(t.getIndexes()));
            cache.setRowEstimate(t.getRowEstimate());
            cache.setVersion(cache.getVersion() == null ? 1 : cache.getVersion() + 1);
            cache.setCachedAt(now);
            schemaCacheRepository.save(cache);
        }

        // 2. 清理目标库已删除的表
        List<SchemaCache> stale = schemaCacheRepository.findByDataSourceId(dataSourceId).stream()
                .filter(c -> !scanned.contains(c.getTableName()))
                .toList();
        if (!stale.isEmpty()) {
            schemaCacheRepository.deleteAll(stale);
        }

        // 3. upsert 表名列表
        TableListCache list = tableListCacheRepository.findByDataSourceId(dataSourceId)
                .orElseGet(() -> {
                    TableListCache l = new TableListCache();
                    l.setDataSourceId(dataSourceId);
                    return l;
                });
        list.setTableJson(toJson(scanned.stream().sorted().toList()));
        list.setCachedAt(now);
        tableListCacheRepository.save(list);
    }

    private String decrypt(String stored) {
        if (SecureConfigEncryptor.isEncrypted(stored)) {
            return SecureConfigEncryptor.decrypt(stored, SecureConfigEncryptor.getKeyFromEnv());
        }
        return stored;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BaseException(SchemaResultCode.SCAN_EXECUTE_FAILED,
                    SchemaResultCode.SCAN_EXECUTE_FAILED.getMessage(), e);
        }
    }
}
