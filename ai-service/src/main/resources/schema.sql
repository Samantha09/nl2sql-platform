CREATE DATABASE IF NOT EXISTS nl2sql_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE nl2sql_ai;

CREATE TABLE IF NOT EXISTS `prompt_templates` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `template_name` VARCHAR(100) NOT NULL UNIQUE,
    `system_prompt` TEXT NOT NULL,
    `user_prompt_template` TEXT,
    `model` VARCHAR(50) DEFAULT 'local-llm',
    `temperature` DECIMAL(2,1) DEFAULT 0.7,
    `max_tokens` INT DEFAULT 2000,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `conversation_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `conversation_id` VARCHAR(64) NOT NULL,
    `role` VARCHAR(20) NOT NULL COMMENT 'user/assistant/system',
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
