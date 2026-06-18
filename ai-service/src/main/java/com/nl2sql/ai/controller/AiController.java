package com.nl2sql.ai.controller;

import com.nl2sql.ai.dto.ConvertRequest;
import com.nl2sql.ai.service.MockLLMService;
import com.nl2sql.common.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final MockLLMService mockLLMService;

    @PostMapping("/convert")
    public R<String> convert(@Valid @RequestBody ConvertRequest request) {
        String sql = mockLLMService.convert(request.getNaturalLanguage(), request.getDataSourceId());
        return R.ok(sql);
    }

    @PostMapping("/validate")
    public R<Boolean> validate(@RequestBody String sql) {
        return R.ok(sql.toLowerCase().startsWith("select"));
    }
}
