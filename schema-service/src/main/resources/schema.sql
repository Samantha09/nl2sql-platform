CREATE DATABASE IF NOT EXISTS nl2sql_schema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE nl2sql_schema;

CREATE TABLE IF NOT EXISTS `data_sources` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '数据源名称',
    `type` VARCHAR(20) NOT NULL COMMENT '类型: mysql/postgresql',
    `host` VARCHAR(255) NOT NULL,
    `port` INT NOT NULL,
    `database_name` TEXT NOT NULL COMMENT '关联的数据库名 JSON 数组',
    `username` VARCHAR(100) NOT NULL,
    `password_encrypted` VARCHAR(500) NOT NULL COMMENT '加密存储',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `schema_cache` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `data_source_id` BIGINT NOT NULL,
    `database_name` VARCHAR(100) NOT NULL COMMENT '数据库名',
    `table_name` VARCHAR(100) NOT NULL,
    `table_comment` VARCHAR(500) DEFAULT '',
    `column_json` TEXT COMMENT '字段详情JSON',
    `primary_key_json` TEXT COMMENT '主键JSON',
    `foreign_key_json` TEXT COMMENT '外键JSON',
    `index_json` TEXT COMMENT '索引JSON',
    `row_estimate` BIGINT DEFAULT 0 COMMENT '行数估算',
    `cached_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `version` INT DEFAULT 1 COMMENT '版本号，用于增量更新',
    UNIQUE KEY `uk_ds_db_table` (`data_source_id`, `database_name`, `table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `table_list_cache` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `data_source_id` BIGINT NOT NULL,
    `table_json` TEXT COMMENT '按数据库分组的表列表JSON Map',
    `cached_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
