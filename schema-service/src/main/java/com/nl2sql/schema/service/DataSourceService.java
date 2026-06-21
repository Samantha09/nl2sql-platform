package com.nl2sql.schema.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.common.cache.CacheNames;
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

import java.util.ArrayList;
import java.util.List;

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

    /** 读已持久化的表名列表（由扫描写入 table_list_cache）。 */
    @Cacheable(cacheNames = CacheNames.SCHEMA_TABLES, key = "#dataSourceId")
    public List<String> scanTables(Long dataSourceId) {
        return tableListCacheRepository.findByDataSourceId(dataSourceId)
                .map(c -> readList(c.getTableJson(), new TypeReference<List<String>>() {}))
                .orElseGet(List::of);
    }

    /** 读已持久化的单表结构详情（由扫描写入 schema_cache）。 */
    @Cacheable(cacheNames = CacheNames.SCHEMA_TABLE, key = "#dataSourceId + ':' + #tableName")
    public TableSchemaDTO getTableDetail(Long dataSourceId, String tableName) {
        SchemaCache cache = schemaCacheRepository
                .findByDataSourceIdAndTableName(dataSourceId, tableName)
                .orElseThrow(() -> new BaseException(SchemaResultCode.DATASOURCE_NOT_FOUND));
        return toDto(cache);
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
}
