package com.nl2sql.schema.entity;

import com.nl2sql.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "data_sources")
public class DataSourceConfig extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false, name = "database_name")
    private String databaseName;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, name = "password_encrypted")
    private String passwordEncrypted;
}
