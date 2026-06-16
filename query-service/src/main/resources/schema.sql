CREATE DATABASE IF NOT EXISTS nl2sql_query CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE nl2sql_query;

CREATE TABLE IF NOT EXISTS `query_conversations` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `conversation_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '会话ID',
    `user_id` BIGINT NOT NULL,
    `data_source_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `query_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `conversation_id` VARCHAR(64) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `data_source_id` BIGINT NOT NULL,
    `natural_language` VARCHAR(1000) NOT NULL COMMENT '原始问题',
    `generated_sql` TEXT NOT NULL COMMENT '生成的SQL',
    `sql_executed` TEXT COMMENT '实际执行的SQL',
    `execute_time_ms` BIGINT DEFAULT 0,
    `result_count` INT DEFAULT 0,
    `status` VARCHAR(20) DEFAULT 'success' COMMENT 'success/failed/timeout',
    `error_message` TEXT,
    `chart_type` VARCHAR(20) DEFAULT 'table' COMMENT 'table/line/bar/pie',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_conversation` (`conversation_id`),
    INDEX `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `query_statistics` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `user_id` BIGINT,
    `query_count` INT DEFAULT 0 COMMENT '查询次数',
    `success_count` INT DEFAULT 0,
    `failed_count` INT DEFAULT 0,
    `avg_execute_time_ms` BIGINT DEFAULT 0,
    `popular_questions_json` TEXT COMMENT '热门问题JSON',
    UNIQUE KEY `uk_date_user` (`stat_date`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `notify_records` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL COMMENT 'feishu/email',
    `recipient` VARCHAR(100) NOT NULL,
    `title` VARCHAR(200),
    `content` TEXT,
    `status` VARCHAR(20) DEFAULT 'pending',
    `sent_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
