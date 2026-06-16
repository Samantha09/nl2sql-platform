package com.nl2sql.schema.repository;

import com.nl2sql.schema.entity.DataSourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSourceConfig, Long> {
}
