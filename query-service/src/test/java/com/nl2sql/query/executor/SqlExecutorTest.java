package com.nl2sql.query.executor;

import com.nl2sql.query.dto.QueryResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqlExecutor - 执行与结果解析（H2 内存库）")
class SqlExecutorTest {

    private SqlExecutor executor;
    private Connection h2;

    @BeforeEach
    void setUp() throws SQLException {
        executor = new SqlExecutor();
        h2 = DriverManager.getConnection("jdbc:h2:mem:test;MODE=MySQL", "sa", "");
        try (Statement st = h2.createStatement()) {
            st.execute("CREATE TABLE orders (id INT, product_name VARCHAR(50), amount INT)");
            st.execute("INSERT INTO orders VALUES (1,'A',100),(2,'B',200)");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        h2.close();
    }

    @Test
    @DisplayName("doExecute 应把结果集转为 List<Map> 并统计行数")
    void shouldMapResultSet() throws SQLException {
        QueryResult r = executor.doExecute(h2, "SELECT product_name, amount FROM orders ORDER BY id");
        assertThat(r.getData()).hasSize(2);
        assertThat(r.getData().get(0)).containsEntry("PRODUCT_NAME", "A").containsEntry("AMOUNT", 100);
        assertThat(r.getTotalCount()).isEqualTo(2);
        assertThat(r.getExecuteTimeMs()).isNotNull();
    }

    @Test
    @DisplayName("两列（维度+数值）结果应推断为 bar 图")
    void shouldInferBarChartForDimensionPlusNumeric() throws SQLException {
        QueryResult r = executor.doExecute(h2, "SELECT product_name, amount FROM orders LIMIT 1");
        assertThat(r.getChartType()).isEqualTo("bar");
    }

    @Test
    @DisplayName("多列结果应推断为 table")
    void shouldInferTableForMultiColumn() throws SQLException {
        QueryResult r = executor.doExecute(h2, "SELECT * FROM orders");
        assertThat(r.getChartType()).isEqualTo("table");
    }
}
