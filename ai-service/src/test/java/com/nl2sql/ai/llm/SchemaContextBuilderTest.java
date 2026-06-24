package com.nl2sql.ai.llm;

import com.nl2sql.common.R;
import com.nl2sql.common.dto.SchemaContextDTO;
import com.nl2sql.common.feign.SchemaServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("SchemaContextBuilder - DDL 摘要拼接")
@ExtendWith(MockitoExtension.class)
class SchemaContextBuilderTest {

    @Mock private SchemaServiceClient schemaServiceClient;
    @InjectMocks private SchemaContextBuilder builder;

    @Test
    @DisplayName("build 应把表/列拼成中文 schema 描述")
    void shouldBuildContextText() {
        SchemaContextDTO ctx = new SchemaContextDTO();
        ctx.setDatabase("shop");
        SchemaContextDTO.TableBrief t = new SchemaContextDTO.TableBrief();
        t.setTableName("orders");
        t.setTableComment("订单表");
        SchemaContextDTO.ColumnBrief c = new SchemaContextDTO.ColumnBrief();
        c.setName("amount"); c.setType("decimal(10,2)");
        t.getColumns().add(c);
        ctx.getTables().add(t);
        when(schemaServiceClient.getSchema(1L, "shop")).thenReturn(R.ok(ctx));

        String text = builder.build(1L, "shop");

        assertThat(text).contains("shop").contains("orders").contains("amount decimal(10,2)").contains("订单表");
    }
}
