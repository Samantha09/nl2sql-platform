package com.nl2sql.schema.repository;

import com.nl2sql.schema.entity.SchemaCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SchemaCacheRepository - 结构缓存持久化")
class SchemaCacheRepositoryTest {

    @Autowired
    private SchemaCacheRepository repository;

    private SchemaCache cache(Long dsId, String databaseName, String table) {
        SchemaCache c = new SchemaCache();
        c.setDataSourceId(dsId);
        c.setDatabaseName(databaseName);
        c.setTableName(table);
        c.setTableComment("注释");
        c.setColumnJson("[]");
        c.setPrimaryKeyJson("[\"id\"]");
        c.setForeignKeyJson("[]");
        c.setIndexJson("[]");
        c.setRowEstimate(0L);
        c.setVersion(1);
        c.setCachedAt(LocalDateTime.now());
        return c;
    }

    @Test
    @DisplayName("按数据源+数据库+表名查询命中")
    void shouldFindByDataSourceAndDatabaseAndTable() {
        repository.save(cache(1L, "shop", "users"));

        Optional<SchemaCache> found = repository.findByDataSourceIdAndDatabaseNameAndTableName(1L, "shop", "users");

        assertThat(found).isPresent();
        assertThat(found.get().getPrimaryKeyJson()).isEqualTo("[\"id\"]");
    }

    @Test
    @DisplayName("按数据源+数据库列出全部表缓存")
    void shouldListByDataSourceAndDatabase() {
        repository.save(cache(2L, "db1", "a"));
        repository.save(cache(2L, "db1", "b"));
        repository.save(cache(2L, "db2", "c"));

        assertThat(repository.findByDataSourceIdAndDatabaseName(2L, "db1")).hasSize(2);
        assertThat(repository.findByDataSourceIdAndDatabaseName(2L, "db2")).hasSize(1);
    }
}
