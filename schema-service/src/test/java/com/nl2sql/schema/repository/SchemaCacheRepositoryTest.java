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

    private SchemaCache cache(Long dsId, String table) {
        SchemaCache c = new SchemaCache();
        c.setDataSourceId(dsId);
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
    @DisplayName("按数据源+表名查询命中")
    void shouldFindByDataSourceAndTable() {
        repository.save(cache(1L, "users"));

        Optional<SchemaCache> found = repository.findByDataSourceIdAndTableName(1L, "users");

        assertThat(found).isPresent();
        assertThat(found.get().getPrimaryKeyJson()).isEqualTo("[\"id\"]");
    }

    @Test
    @DisplayName("按数据源列出全部表缓存")
    void shouldListByDataSource() {
        repository.save(cache(2L, "a"));
        repository.save(cache(2L, "b"));

        assertThat(repository.findByDataSourceId(2L)).hasSize(2);
    }
}
