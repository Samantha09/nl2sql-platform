package com.nl2sql.schema.repository;

import com.nl2sql.schema.entity.SchemaCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchemaCacheRepository extends JpaRepository<SchemaCache, Long> {

    Optional<SchemaCache> findByDataSourceIdAndDatabaseNameAndTableName(Long dataSourceId, String databaseName, String tableName);

    List<SchemaCache> findByDataSourceIdAndDatabaseName(Long dataSourceId, String databaseName);

    List<SchemaCache> findByDataSourceId(Long dataSourceId);
}
