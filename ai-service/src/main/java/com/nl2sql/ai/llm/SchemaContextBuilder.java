package com.nl2sql.ai.llm;

import com.nl2sql.common.R;
import com.nl2sql.common.dto.SchemaContextDTO;
import com.nl2sql.common.feign.SchemaServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/** 调 schema-service 取精简 schema，拼成喂给 LLM 的系统提示文本。 */
@Component
@RequiredArgsConstructor
public class SchemaContextBuilder {

    private static final String SYSTEM_SUFFIX =
            "\n\n请仅基于以上表结构生成 MySQL 查询 SQL，只返回 SQL 语句本身，不要解释。";

    private final SchemaServiceClient schemaServiceClient;

    /** 取 schema 并拼成 DDL 摘要；取不到时返回空串（交由上层决定是否继续）。 */
    public String build(Long dataSourceId, String databaseName) {
        R<SchemaContextDTO> resp = schemaServiceClient.getSchema(dataSourceId, databaseName);
        if (resp == null || resp.getData() == null) {
            return "";
        }
        return toText(resp.getData());
    }

    private String toText(SchemaContextDTO ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("数据库 ").append(ctx.getDatabase()).append(" 包含以下表结构：\n\n");
        for (SchemaContextDTO.TableBrief t : ctx.getTables()) {
            sb.append("表 ").append(t.getTableName());
            if (t.getTableComment() != null && !t.getTableComment().isBlank()) {
                sb.append("（").append(t.getTableComment()).append("）");
            }
            sb.append("：");
            String cols = t.getColumns().stream()
                    .map(c -> c.getName() + " " + c.getType())
                    .collect(Collectors.joining(", "));
            sb.append(cols).append("\n");
        }
        sb.append(SYSTEM_SUFFIX);
        return sb.toString();
    }
}
