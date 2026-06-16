package com.nl2sql.schema.service;

import com.nl2sql.schema.dto.TableSchemaDTO;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.repository.DataSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceRepository repository;

    public DataSourceConfig create(DataSourceConfig config) {
        return repository.save(config);
    }

    public List<DataSourceConfig> list() {
        return repository.findAll();
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<String> scanTables(Long dataSourceId) {
        // 骨架阶段返回 Mock 数据
        return List.of("users", "orders", "products");
    }

    public TableSchemaDTO getTableDetail(Long dataSourceId, String tableName) {
        TableSchemaDTO dto = new TableSchemaDTO();
        dto.setTableName(tableName);
        dto.setTableComment("Mock 表注释");
        TableSchemaDTO.ColumnInfo c1 = new TableSchemaDTO.ColumnInfo();
        c1.setName("id");
        c1.setType("BIGINT");
        c1.setComment("主键");
        TableSchemaDTO.ColumnInfo c2 = new TableSchemaDTO.ColumnInfo();
        c2.setName("name");
        c2.setType("VARCHAR");
        c2.setComment("名称");
        dto.setColumns(List.of(c1, c2));
        dto.setPrimaryKeys(List.of("id"));
        return dto;
    }
}
