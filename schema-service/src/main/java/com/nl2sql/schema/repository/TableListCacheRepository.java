package com.nl2sql.schema.repository;

import com.nl2sql.schema.entity.TableListCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TableListCacheRepository extends JpaRepository<TableListCache, Long> {

    Optional<TableListCache> findByDataSourceId(Long dataSourceId);
}
