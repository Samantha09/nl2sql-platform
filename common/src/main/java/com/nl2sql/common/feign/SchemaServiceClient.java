package com.nl2sql.common.feign;

import com.nl2sql.common.R;
import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.dto.SchemaContextDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * schema-service 内部接口客户端。仅供服务间调用，对应 /api/schema/internal/**。
 */
@FeignClient(name = "schema-service", path = "/api/schema/internal")
public interface SchemaServiceClient {

    /** 取目标数据源解密后的连接信息 */
    @GetMapping("/datasource/{id}/connection")
    R<DataSourceConnectionDTO> getConnection(@PathVariable("id") Long id);

    /** 取指定库的精简 schema 上下文 */
    @GetMapping("/datasource/{id}/schema")
    R<SchemaContextDTO> getSchema(@PathVariable("id") Long id, @RequestParam("database") String database);
}
