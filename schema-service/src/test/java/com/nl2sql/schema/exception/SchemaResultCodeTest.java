package com.nl2sql.schema.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SchemaResultCode - 领域错误码")
class SchemaResultCodeTest {

    @Test
    @DisplayName("错误码落在 schema 领域 band（11xxx）且绑定 i18n key")
    void shouldExposeCodeAndKey() {
        assertThat(SchemaResultCode.DB_TYPE_UNSUPPORTED.getCode()).isEqualTo(11001);
        assertThat(SchemaResultCode.DB_TYPE_UNSUPPORTED.getI18nKey()).isEqualTo("schema.db_type_unsupported");
        assertThat(SchemaResultCode.SCAN_CONNECT_FAILED.getDesc()).isEqualTo("无法连接目标数据库");
    }
}
