package com.nl2sql.schema.scanner.impl;

import com.nl2sql.common.exception.BaseException;
import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.exception.SchemaResultCode;
import com.nl2sql.schema.scanner.DatabaseScanner;
import com.nl2sql.schema.scanner.ScanContext;
import com.nl2sql.schema.scanner.model.ColumnMetadata;
import com.nl2sql.schema.scanner.model.ForeignKeyMetadata;
import com.nl2sql.schema.scanner.model.IndexMetadata;
import com.nl2sql.schema.scanner.model.SchemaMetadata;
import com.nl2sql.schema.scanner.model.TableMetadata;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** MySQL 方言实现：全部元数据查 information_schema。 */
@Component
public class MySqlDatabaseScanner implements DatabaseScanner {

    private static final String URL_TEMPLATE =
            "jdbc:mysql://%s:%d/%s?useInformationSchema=true&connectTimeout=5000&socketTimeout=30000"
            + "&useSSL=false&allowPublicKeyRetrieval=true";

    @Override
    public boolean supports(DbType type) {
        return type == DbType.MYSQL;
    }

    @Override
    public List<String> listTables(ScanContext ctx) {
        try (Connection conn = open(ctx)) {
            return queryTableNames(conn, ctx.databaseName());
        } catch (SQLException e) {
            throw connectFailed(e);
        }
    }

    @Override
    public SchemaMetadata scan(ScanContext ctx) {
        SchemaMetadata meta = new SchemaMetadata();
        try (Connection conn = open(ctx)) {
            String db = ctx.databaseName();
            for (TableMetadata table : queryTables(conn, db)) {
                table.setColumns(queryColumns(conn, db, table));
                table.setIndexes(queryIndexes(conn, db, table.getName()));
                table.setForeignKeys(queryForeignKeys(conn, db, table.getName()));
                meta.getTables().add(table);
            }
            return meta;
        } catch (SQLException e) {
            throw executeFailed(e);
        }
    }

    private Connection open(ScanContext ctx) throws SQLException {
        String url = String.format(URL_TEMPLATE, ctx.host(), ctx.port(), ctx.databaseName());
        return DriverManager.getConnection(url, ctx.username(), ctx.password());
    }

    private List<String> queryTableNames(Connection conn, String db) throws SQLException {
        String sql = "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name";
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, db);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("table_name"));
                }
            }
        }
        return names;
    }

    private List<TableMetadata> queryTables(Connection conn, String db) throws SQLException {
        String sql = "SELECT table_name, table_comment, table_rows FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name";
        List<TableMetadata> tables = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, db);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableMetadata t = new TableMetadata();
                    t.setName(rs.getString("table_name"));
                    t.setComment(nullToEmpty(rs.getString("table_comment")));
                    t.setRowEstimate(rs.getLong("table_rows"));
                    tables.add(t);
                }
            }
        }
        return tables;
    }

    private List<ColumnMetadata> queryColumns(Connection conn, String db, TableMetadata table)
            throws SQLException {
        String sql = "SELECT column_name, column_type, column_comment, is_nullable, "
                + "column_default, ordinal_position, column_key FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position";
        List<ColumnMetadata> columns = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, db);
            ps.setString(2, table.getName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ColumnMetadata c = new ColumnMetadata();
                    c.setName(rs.getString("column_name"));
                    c.setType(rs.getString("column_type"));
                    c.setComment(nullToEmpty(rs.getString("column_comment")));
                    c.setNullable("YES".equalsIgnoreCase(rs.getString("is_nullable")));
                    c.setDefaultValue(rs.getString("column_default"));
                    c.setOrdinalPosition(rs.getInt("ordinal_position"));
                    columns.add(c);
                    if ("PRI".equalsIgnoreCase(rs.getString("column_key"))) {
                        table.getPrimaryKeys().add(c.getName());
                    }
                }
            }
        }
        return columns;
    }

    private List<IndexMetadata> queryIndexes(Connection conn, String db, String tableName)
            throws SQLException {
        String sql = "SELECT index_name, non_unique, seq_in_index, column_name "
                + "FROM information_schema.statistics WHERE table_schema = ? AND table_name = ? "
                + "ORDER BY index_name, seq_in_index";
        Map<String, IndexMetadata> byName = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, db);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("index_name");
                    IndexMetadata idx = byName.computeIfAbsent(name, n -> {
                        IndexMetadata m = new IndexMetadata();
                        m.setName(n);
                        return m;
                    });
                    idx.setUnique(rs.getInt("non_unique") == 0);
                    idx.getColumns().add(rs.getString("column_name"));
                }
            }
        }
        return new ArrayList<>(byName.values());
    }

    private List<ForeignKeyMetadata> queryForeignKeys(Connection conn, String db, String tableName)
            throws SQLException {
        String sql = "SELECT constraint_name, column_name, referenced_table_name, "
                + "referenced_column_name FROM information_schema.key_column_usage "
                + "WHERE table_schema = ? AND table_name = ? AND referenced_table_name IS NOT NULL "
                + "ORDER BY constraint_name, ordinal_position";
        Map<String, ForeignKeyMetadata> byName = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, db);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("constraint_name");
                    ForeignKeyMetadata fk = byName.computeIfAbsent(name, n -> {
                        ForeignKeyMetadata m = new ForeignKeyMetadata();
                        m.setName(n);
                        return m;
                    });
                    fk.setReferencedTable(rs.getString("referenced_table_name"));
                    fk.getColumns().add(rs.getString("column_name"));
                    fk.getReferencedColumns().add(rs.getString("referenced_column_name"));
                }
            }
        }
        return new ArrayList<>(byName.values());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private BaseException connectFailed(SQLException e) {
        return new BaseException(SchemaResultCode.SCAN_CONNECT_FAILED,
                SchemaResultCode.SCAN_CONNECT_FAILED.getMessage(), e);
    }

    private BaseException executeFailed(SQLException e) {
        return new BaseException(SchemaResultCode.SCAN_EXECUTE_FAILED,
                SchemaResultCode.SCAN_EXECUTE_FAILED.getMessage(), e);
    }
}
