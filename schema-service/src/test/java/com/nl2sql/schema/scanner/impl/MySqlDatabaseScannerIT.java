package com.nl2sql.schema.scanner.impl;

import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.scanner.ScanContext;
import com.nl2sql.schema.scanner.model.ColumnMetadata;
import com.nl2sql.schema.scanner.model.SchemaMetadata;
import com.nl2sql.schema.scanner.model.TableMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("MySqlDatabaseScanner - 真实 MySQL 元数据抽取")
class MySqlDatabaseScannerIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("shop")
            .withUsername("root_test")
            .withPassword("test_pwd");

    @BeforeAll
    static void seed() throws Exception {
        try (Connection c = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE users (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
                    name VARCHAR(100) NOT NULL COMMENT '用户名',
                    email VARCHAR(200) NULL,
                    UNIQUE KEY uk_email (email)
                ) COMMENT='用户表'
                """);
            s.execute("""
                CREATE TABLE orders (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL COMMENT '下单用户',
                    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
                ) COMMENT='订单表'
                """);
        }
    }

    private ScanContext ctx() {
        return new ScanContext(DbType.MYSQL, MYSQL.getHost(),
                MYSQL.getMappedPort(MySQLContainer.MYSQL_PORT), "shop",
                MYSQL.getUsername(), MYSQL.getPassword());
    }

    @Test
    @DisplayName("scan 抽取表注释、列(含可空/注释)、主键、唯一索引、外键")
    void shouldScanFullMetadata() {
        SchemaMetadata meta = new MySqlDatabaseScanner().scan(ctx());

        TableMetadata users = meta.getTables().stream()
                .filter(t -> t.getName().equals("users")).findFirst().orElseThrow();
        assertThat(users.getComment()).isEqualTo("用户表");
        assertThat(users.getPrimaryKeys()).containsExactly("id");

        ColumnMetadata name = users.getColumns().stream()
                .filter(col -> col.getName().equals("name")).findFirst().orElseThrow();
        assertThat(name.getType()).isEqualTo("varchar(100)");
        assertThat(name.getComment()).isEqualTo("用户名");
        assertThat(name.isNullable()).isFalse();

        assertThat(users.getIndexes()).anySatisfy(idx -> {
            assertThat(idx.getName()).isEqualTo("uk_email");
            assertThat(idx.isUnique()).isTrue();
            assertThat(idx.getColumns()).containsExactly("email");
        });

        TableMetadata orders = meta.getTables().stream()
                .filter(t -> t.getName().equals("orders")).findFirst().orElseThrow();
        assertThat(orders.getForeignKeys()).anySatisfy(fk -> {
            assertThat(fk.getColumns()).containsExactly("user_id");
            assertThat(fk.getReferencedTable()).isEqualTo("users");
            assertThat(fk.getReferencedColumns()).containsExactly("id");
        });
    }

    @Test
    @DisplayName("listTables 返回全部表名")
    void shouldListTables() {
        assertThat(new MySqlDatabaseScanner().listTables(ctx()))
                .containsExactlyInAnyOrder("users", "orders");
    }
}
