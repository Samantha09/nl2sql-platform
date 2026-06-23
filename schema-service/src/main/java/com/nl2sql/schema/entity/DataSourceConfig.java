package com.nl2sql.schema.entity;

import com.nl2sql.common.entity.BaseEntity;
import com.nl2sql.schema.converter.StringListJsonConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

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

    /** 该数据源关联的数据库名列表，以 JSON 数组形式存入 database_name 列。 */
    @Column(name = "database_name", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = StringListJsonConverter.class)
    private List<String> databaseNames = new ArrayList<>();

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, name = "password_encrypted")
    private String passwordEncrypted;
}
