package com.nl2sql.schema.scanner;

import com.nl2sql.schema.enums.DbType;

/**
 * 一次扫描的输入。password 为已解密明文，仅存活于内存，不落库不打日志。
 *
 * @param type         数据库类型
 * @param host         主机
 * @param port         端口
 * @param databaseName 目标库名
 * @param username     连接用户名
 * @param password     已解密的明文密码
 */
public record ScanContext(
        DbType type,
        String host,
        int port,
        String databaseName,
        String username,
        String password
) {}
