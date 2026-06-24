package com.nl2sql.common.feign;

import com.nl2sql.common.R;
import com.nl2sql.common.dto.ConvertRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * ai-service NL → SQL 客户端。
 */
@FeignClient(name = "ai-service", path = "/api/ai")
public interface AiServiceClient {

    @PostMapping("/convert")
    R<String> convert(@RequestBody ConvertRequest request);
}
