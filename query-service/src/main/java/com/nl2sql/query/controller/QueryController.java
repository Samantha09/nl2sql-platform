package com.nl2sql.query.controller;

import com.nl2sql.common.R;
import com.nl2sql.common.PageResult;
import com.nl2sql.query.dto.QueryRequest;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.entity.QueryHistory;
import com.nl2sql.query.service.QueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping("/nl")
    public R<QueryResult> nlQuery(@Valid @RequestBody QueryRequest request) {
        return R.ok(queryService.queryByNaturalLanguage(request));
    }

    @PostMapping("/sql")
    public R<QueryResult> sqlQuery(@RequestBody String sql) {
        return R.ok(queryService.queryBySql(sql));
    }

    @GetMapping("/history")
    public R<List<QueryHistory>> history(@RequestParam String conversationId) {
        return R.ok(queryService.history(conversationId));
    }

    @GetMapping("/history/page")
    public R<PageResult<QueryHistory>> historyPage(
            @RequestParam String conversationId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(queryService.historyPage(conversationId, pageNum, pageSize));
    }

    @GetMapping("/statistics")
    public R<String> statistics() {
        return R.ok("statistics placeholder");
    }
}
