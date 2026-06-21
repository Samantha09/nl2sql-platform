package com.nl2sql.schema.repository;

import com.nl2sql.schema.entity.SchemaCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchemaCacheRepository extends JpaRepository<SchemaCache, Long> {

    Optional<SchemaCache> findByDataSourceIdAndTableName(Long dataSourceId, String tableName);

    List<SchemaCache> findByDataSourceId(Long dataSourceId);
}
