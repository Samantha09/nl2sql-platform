package com.nl2sql.schema.scanner;

import com.nl2sql.common.exception.BaseException;
import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.exception.SchemaResultCode;
import org.springframework.stereotype.Component;

import java.util.List;

/** 收集所有 {@link DatabaseScanner} 实现，按 {@link DbType} 分发。 */
@Component
public class ScannerRegistry {

    private final List<DatabaseScanner> scanners;

    public ScannerRegistry(List<DatabaseScanner> scanners) {
        this.scanners = scanners;
    }

    public DatabaseScanner resolve(DbType type) {
        return scanners.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> new BaseException(SchemaResultCode.DB_TYPE_UNSUPPORTED));
    }
}
