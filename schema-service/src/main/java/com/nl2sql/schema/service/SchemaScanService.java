package com.nl2sql.schema.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.*;
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
    public Map<String, List<String>> scan(Long dataSourceId) {
        DataSourceConfig ds = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new BaseException(SchemaResultCode.DATASOURCE_NOT_FOUND));

        DbType type = IEnum.of(DbType.class, ds.getType())
                .orElseThrow(() -> new BaseException(SchemaResultCode.DB_TYPE_UNSUPPORTED));

        String password = decrypt(ds.getPasswordEncrypted());
        Map<String, SchemaMetadata> result = new LinkedHashMap<>();
        for (String dbName : ds.getDatabaseNames()) {
            ScanContext ctx = new ScanContext(type, ds.getHost(), ds.getPort(), dbName, ds.getUsername(), password);
            result.put(dbName, scannerRegistry.resolve(type).scan(ctx));
        }

        persist(dataSourceId, result);

        return result.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getTables().stream().map(TableMetadata::getName).sorted().toList(),
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    private void persist(Long dataSourceId, Map<String, SchemaMetadata> result) {
        LocalDateTime now = LocalDateTime.now();

        // 1. upsert 每张表（按 databaseName + tableName）
        for (Map.Entry<String, SchemaMetadata> entry : result.entrySet()) {
            String dbName = entry.getKey();
            for (TableMetadata t : entry.getValue().getTables()) {
                SchemaCache cache = schemaCacheRepository
                        .findByDataSourceIdAndDatabaseNameAndTableName(dataSourceId, dbName, t.getName())
                        .orElseGet(() -> {
                            SchemaCache c = new SchemaCache();
                            c.setDataSourceId(dataSourceId);
                            c.setDatabaseName(dbName);
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
        }

        // 2. 清理本次未扫描到的（databaseName, tableName）组合
        Set<String> scannedKeys = result.entrySet().stream()
                .flatMap(e -> e.getValue().getTables().stream()
                        .map(t -> key(e.getKey(), t.getName())))
                .collect(Collectors.toSet());
        List<SchemaCache> stale = schemaCacheRepository.findByDataSourceId(dataSourceId).stream()
                .filter(c -> !scannedKeys.contains(key(c.getDatabaseName(), c.getTableName())))
                .toList();
        if (!stale.isEmpty()) {
            schemaCacheRepository.deleteAll(stale);
        }

        // 3. upsert 表名分组列表
        Map<String, List<String>> tablesByDb = result.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getTables().stream().map(TableMetadata::getName).sorted().toList(),
                        (a, b) -> a,
                        LinkedHashMap::new));
        TableListCache list = tableListCacheRepository.findByDataSourceId(dataSourceId)
                .orElseGet(() -> {
                    TableListCache l = new TableListCache();
                    l.setDataSourceId(dataSourceId);
                    return l;
                });
        list.setTableJson(toJson(tablesByDb));
        list.setCachedAt(now);
        tableListCacheRepository.save(list);
    }

    private static String key(String databaseName, String tableName) {
        return databaseName + "." + tableName;
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
