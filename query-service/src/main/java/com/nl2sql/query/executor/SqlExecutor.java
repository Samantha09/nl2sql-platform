package com.nl2sql.query.executor;

import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.exception.BaseException;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.exception.QueryResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 在目标库执行 SQL 并解析结果。MySQL 即连即用；核心解析逻辑 {@link #doExecute} 可注入连接单测。
 * 本期无只读护栏（见设计文档「已知风险」）：被查库应使用只读账号。
 */
@Slf4j
@Component
public class SqlExecutor {

    private static final String MYSQL_URL_TEMPLATE =
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=30000";

    /** 建连并执行；连接失败与执行失败分别映射不同错误码。 */
    public QueryResult execute(DataSourceConnectionDTO conn, String database, String sql) {
        if (conn == null || !"mysql".equalsIgnoreCase(conn.getType())) {
            throw new BaseException(QueryResultCode.QUERY_DB_TYPE_UNSUPPORTED);
        }
        String url = String.format(MYSQL_URL_TEMPLATE, conn.getHost(), conn.getPort(), database);
        try (Connection jdbc = DriverManager.getConnection(url, conn.getUsername(), conn.getPassword())) {
            return doExecute(jdbc, sql);
        } catch (SQLException e) {
            throw new BaseException(QueryResultCode.QUERY_DATASOURCE_CONNECT_FAILED,
                    QueryResultCode.QUERY_DATASOURCE_CONNECT_FAILED.getMessage(), e);
        }
    }

    /** 解析 ResultSet → QueryResult。包级可见以支持注入连接的单测。 */
    QueryResult doExecute(Connection jdbc, String sql) throws SQLException {
        long start = System.currentTimeMillis();
        try (Statement stmt = jdbc.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= n; i++) {
                    row.put(md.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
            QueryResult result = new QueryResult();
            result.setSql(sql);
            result.setData(rows);
            result.setTotalCount(rows.size());
            result.setExecuteTimeMs(System.currentTimeMillis() - start);
            result.setChartType(inferChartType(md, n));
            return result;
        } catch (SQLException e) {
            throw new BaseException(QueryResultCode.QUERY_SQL_EXECUTE_FAILED,
                    QueryResultCode.QUERY_SQL_EXECUTE_FAILED.getMessage(), e);
        }
    }

    /** 简单启发：恰好两列且第二列为数值 → bar；否则 table。 */
    private String inferChartType(ResultSetMetaData md, int n) throws SQLException {
        if (n == 2) {
            String t = md.getColumnTypeName(2).toUpperCase();
            if (t.contains("INT") || t.contains("DECIMAL") || t.contains("DOUBLE")
                    || t.contains("FLOAT") || t.contains("NUMERIC")) {
                return "bar";
            }
        }
        return "table";
    }
}
