package com.nl2sql.schema.controller;

import com.nl2sql.common.R;
import com.nl2sql.common.cache.CacheNames;
import com.nl2sql.schema.dto.TableSchemaDTO;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.service.DataSourceService;
import com.nl2sql.schema.service.SchemaScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService service;
    private final SchemaScanService scanService;

    @PostMapping("/datasource")
    public R<DataSourceConfig> add(@RequestBody DataSourceConfig config) {
        return R.ok(service.create(config));
    }

    @GetMapping("/datasource/list")
    public R<List<DataSourceConfig>> list() {
        return R.ok(service.list());
    }

    @DeleteMapping("/datasource/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    /** 触发真实扫描并持久化；清除该数据源的表列表与表详情缓存。 */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.SCHEMA_TABLES, key = "#datasourceId"),
            @CacheEvict(cacheNames = CacheNames.SCHEMA_TABLE, allEntries = true)
    })
    @PostMapping("/scan/{datasourceId}")
    public R<List<String>> scan(@PathVariable Long datasourceId) {
        return R.ok(scanService.scan(datasourceId));
    }

    @GetMapping("/{datasourceId}/tables")
    public R<List<String>> tables(@PathVariable Long datasourceId) {
        return R.ok(service.scanTables(datasourceId));
    }

    @GetMapping("/{datasourceId}/tables/{tableName}")
    public R<TableSchemaDTO> tableDetail(@PathVariable Long datasourceId, @PathVariable String tableName) {
        return R.ok(service.getTableDetail(datasourceId, tableName));
    }
}
