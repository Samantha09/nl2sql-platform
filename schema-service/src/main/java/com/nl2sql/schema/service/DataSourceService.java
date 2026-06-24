package com.nl2sql.schema.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.common.cache.CacheNames;
import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.dto.SchemaContextDTO;
import com.nl2sql.common.encrypt.SecureConfigEncryptor;
import com.nl2sql.common.exception.BaseException;
import com.nl2sql.schema.dto.TableSchemaDTO;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.entity.SchemaCache;
import com.nl2sql.schema.exception.SchemaResultCode;
import com.nl2sql.schema.repository.DataSourceRepository;
import com.nl2sql.schema.repository.SchemaCacheRepository;
import com.nl2sql.schema.repository.TableListCacheRepository;
import com.nl2sql.schema.scanner.model.ColumnMetadata;
import com.nl2sql.schema.scanner.model.ForeignKeyMetadata;
import com.nl2sql.schema.scanner.model.IndexMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceRepository repository;
    private final SchemaCacheRepository schemaCacheRepository;
    private final TableListCacheRepository tableListCacheRepository;
    private final ObjectMapper objectMapper;

    @CacheEvict(cacheNames = CacheNames.DS_LIST, allEntries = true)
    public DataSourceConfig create(DataSourceConfig config) {
        return repository.save(config);
    }

    @Cacheable(cacheNames = CacheNames.DS_LIST)
    public List<DataSourceConfig> list() {
        return repository.findAll();
    }

    @CacheEvict(cacheNames = CacheNames.DS_LIST, allEntries = true)
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @CacheEvict(cacheNames = CacheNames.DS_LIST, allEntries = true)
    public DataSourceConfig update(Long id, DataSourceConfig config) {
        DataSourceConfig existing = repository.findById(id)
                .orElseThrow(() -> new BaseException(SchemaResultCode.DATASOURCE_NOT_FOUND));
        existing.setName(config.getName());
        existing.setType(config.getType());
        existing.setHost(config.getHost());
        existing.setPort(config.getPort());
        existing.setDatabaseNames(config.getDatabaseNames());
        existing.setUsername(config.getUsername());
        if (config.getPasswordEncrypted() != null && !config.getPasswordEncrypted().isBlank()) {
            existing.setPasswordEncrypted(config.getPasswordEncrypted());
        }
        return repository.save(existing);
    }

    /** 读已持久化的表名列表（按数据库名分组）。 */
    @Cacheable(cacheNames = CacheNames.SCHEMA_TABLES, key = "#dataSourceId")
    public Map<String, List<String>> scanTables(Long dataSourceId) {
        return tableListCacheRepository.findByDataSourceId(dataSourceId)
                .map(c -> readMap(c.getTableJson()))
                .orElseGet(LinkedHashMap::new);
    }

    /** 读已持久化的单表结构详情（由扫描写入 schema_cache）。 */
    @Cacheable(cacheNames = CacheNames.SCHEMA_TABLE, key = "#dataSourceId + ':' + #databaseName + ':' + #tableName")
    public TableSchemaDTO getTableDetail(Long dataSourceId, String databaseName, String tableName) {
        SchemaCache cache = schemaCacheRepository
                .findByDataSourceIdAndDatabaseNameAndTableName(dataSourceId, databaseName, tableName)
                .orElseThrow(() -> new BaseException(SchemaResultCode.DATASOURCE_NOT_FOUND));
        return toDto(cache);
    }

    /** 内部接口：返回解密后的目标库连接信息，供 query-service 建连执行。 */
    public DataSourceConnectionDTO getConnection(Long dataSourceId) {
        DataSourceConfig ds = repository.findById(dataSourceId)
                .orElseThrow(() -> new BaseException(SchemaResultCode.DATASOURCE_NOT_FOUND));
        DataSourceConnectionDTO dto = new DataSourceConnectionDTO();
        dto.setType(ds.getType());
        dto.setHost(ds.getHost());
        dto.setPort(ds.getPort());
        dto.setDatabaseNames(ds.getDatabaseNames());
        dto.setUsername(ds.getUsername());
        dto.setPassword(decryptPassword(ds.getPasswordEncrypted()));
        return dto;
    }

    /** 内部接口：从已持久化的 schema_cache 组装精简 schema，供 ai-service 喂 LLM。 */
    public SchemaContextDTO getSchemaContext(Long dataSourceId, String databaseName) {
        List<SchemaCache> caches = schemaCacheRepository
                .findByDataSourceIdAndDatabaseName(dataSourceId, databaseName);
        SchemaContextDTO dto = new SchemaContextDTO();
        dto.setDatabase(databaseName);
        dto.setTables(caches.stream().map(this::toTableBrief).toList());
        return dto;
    }

    private SchemaContextDTO.TableBrief toTableBrief(SchemaCache cache) {
        SchemaContextDTO.TableBrief brief = new SchemaContextDTO.TableBrief();
        brief.setTableName(cache.getTableName());
        brief.setTableComment(cache.getTableComment());
        List<ColumnMetadata> cols = readList(cache.getColumnJson(),
                new TypeReference<List<ColumnMetadata>>() {});
        brief.setColumns(cols.stream().map(this::toColumnBrief).toList());
        return brief;
    }

    private SchemaContextDTO.ColumnBrief toColumnBrief(ColumnMetadata m) {
        SchemaContextDTO.ColumnBrief c = new SchemaContextDTO.ColumnBrief();
        c.setName(m.getName());
        c.setType(m.getType());
        c.setComment(m.getComment());
        return c;
    }

    private String decryptPassword(String stored) {
        if (SecureConfigEncryptor.isEncrypted(stored)) {
            return SecureConfigEncryptor.decrypt(stored, SecureConfigEncryptor.getKeyFromEnv());
        }
        return stored;
    }

    private TableSchemaDTO toDto(SchemaCache cache) {
        TableSchemaDTO dto = new TableSchemaDTO();
        dto.setTableName(cache.getTableName());
        dto.setTableComment(cache.getTableComment());
        dto.setPrimaryKeys(readList(cache.getPrimaryKeyJson(), new TypeReference<List<String>>() {}));
        dto.setColumns(readList(cache.getColumnJson(), new TypeReference<List<ColumnMetadata>>() {})
                .stream().map(this::toColumnInfo).toList());
        dto.setIndexes(readList(cache.getIndexJson(), new TypeReference<List<IndexMetadata>>() {})
                .stream().map(this::toIndexInfo).toList());
        dto.setForeignKeys(readList(cache.getForeignKeyJson(), new TypeReference<List<ForeignKeyMetadata>>() {})
                .stream().map(this::toFkInfo).toList());
        return dto;
    }

    private TableSchemaDTO.ColumnInfo toColumnInfo(ColumnMetadata m) {
        TableSchemaDTO.ColumnInfo c = new TableSchemaDTO.ColumnInfo();
        c.setName(m.getName());
        c.setType(m.getType());
        c.setComment(m.getComment());
        c.setNullable(m.isNullable());
        c.setDefaultValue(m.getDefaultValue());
        return c;
    }

    private TableSchemaDTO.IndexInfo toIndexInfo(IndexMetadata m) {
        TableSchemaDTO.IndexInfo i = new TableSchemaDTO.IndexInfo();
        i.setName(m.getName());
        i.setUnique(m.isUnique());
        i.setColumns(m.getColumns());
        return i;
    }

    private TableSchemaDTO.ForeignKeyInfo toFkInfo(ForeignKeyMetadata m) {
        TableSchemaDTO.ForeignKeyInfo f = new TableSchemaDTO.ForeignKeyInfo();
        f.setName(m.getName());
        f.setColumns(m.getColumns());
        f.setReferencedTable(m.getReferencedTable());
        f.setReferencedColumns(m.getReferencedColumns());
        return f;
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> ref) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, ref);
        } catch (JsonProcessingException e) {
            throw new BaseException(SchemaResultCode.SCAN_EXECUTE_FAILED,
                    SchemaResultCode.SCAN_EXECUTE_FAILED.getMessage(), e);
        }
    }

    private Map<String, List<String>> readMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, List<String>>>() {});
        } catch (JsonProcessingException e) {
            throw new BaseException(SchemaResultCode.SCAN_EXECUTE_FAILED,
                    SchemaResultCode.SCAN_EXECUTE_FAILED.getMessage(), e);
        }
    }
}
