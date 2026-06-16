package com.nl2sql.schema.controller;

import com.nl2sql.common.R;
import com.nl2sql.schema.dto.TableSchemaDTO;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.service.DataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService service;

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

    @PostMapping("/scan/{datasourceId}")
    public R<List<String>> scan(@PathVariable Long datasourceId) {
        return R.ok(service.scanTables(datasourceId));
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
