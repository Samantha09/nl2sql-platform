package com.nl2sql.ai.controller;

import com.nl2sql.ai.service.Nl2SqlConvertService;
import com.nl2sql.common.R;
import com.nl2sql.common.dto.ConvertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final Nl2SqlConvertService convertService;

    @PostMapping("/convert")
    public R<String> convert(@Valid @RequestBody ConvertRequest request) {
        return R.ok(convertService.convert(request));
    }

    @PostMapping("/validate")
    public R<Boolean> validate(@RequestBody String sql) {
        return R.ok(sql.toLowerCase().startsWith("select"));
    }
}
