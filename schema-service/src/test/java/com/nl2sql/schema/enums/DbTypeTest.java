package com.nl2sql.schema.enums;

import com.nl2sql.common.enums.IEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DbType - 数据库类型枚举")
class DbTypeTest {

    @Test
    @DisplayName("getCode/getDesc 返回稳定编码与可读描述")
    void shouldExposeCodeAndDesc() {
        assertThat(DbType.MYSQL.getCode()).isEqualTo("mysql");
        assertThat(DbType.MYSQL.getDesc()).isEqualTo("MySQL");
    }

    @Test
    @DisplayName("of 按编码反查命中")
    void shouldResolveByCode() {
        Optional<DbType> found = IEnum.of(DbType.class, "mysql");
        assertThat(found).contains(DbType.MYSQL);
    }

    @Test
    @DisplayName("of 未知编码返回空")
    void shouldReturnEmptyForUnknown() {
        assertThat(IEnum.of(DbType.class, "oracle")).isEmpty();
        assertThat(IEnum.of(DbType.class, (String) null)).isEmpty();
    }
}
