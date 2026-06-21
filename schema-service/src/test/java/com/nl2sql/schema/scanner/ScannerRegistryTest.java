package com.nl2sql.schema.scanner;

import com.nl2sql.common.exception.BaseException;
import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.exception.SchemaResultCode;
import com.nl2sql.schema.scanner.model.SchemaMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ScannerRegistry - 按 DbType 分发")
class ScannerRegistryTest {

    /** 测试用假 scanner，仅声明支持 MYSQL。 */
    static class FakeMySqlScanner implements DatabaseScanner {
        @Override public boolean supports(DbType type) { return type == DbType.MYSQL; }
        @Override public SchemaMetadata scan(ScanContext c) { return new SchemaMetadata(); }
        @Override public List<String> listTables(ScanContext c) { return List.of(); }
    }

    @Test
    @DisplayName("resolve 命中支持的类型")
    void shouldResolveSupported() {
        ScannerRegistry registry = new ScannerRegistry(List.of(new FakeMySqlScanner()));
        assertThat(registry.resolve(DbType.MYSQL)).isInstanceOf(FakeMySqlScanner.class);
    }

    @Test
    @DisplayName("resolve 未命中抛 DB_TYPE_UNSUPPORTED")
    void shouldThrowWhenUnsupported() {
        ScannerRegistry registry = new ScannerRegistry(List.of());
        assertThatThrownBy(() -> registry.resolve(DbType.MYSQL))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getCode())
                        .isEqualTo(SchemaResultCode.DB_TYPE_UNSUPPORTED.getCode()));
    }
}
