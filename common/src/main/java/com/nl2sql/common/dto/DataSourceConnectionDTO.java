package com.nl2sql.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 目标数据源连接信息。schema-service 解密后经内网 Feign 返回，供 query-service 建连执行。
 * password 为明文，仅存活于内存与内网传输，不落库、不打日志。
 */
@Data
public class DataSourceConnectionDTO implements Serializable {

    /** 数据库类型，取 {@code DataSourceConfig.type}（如 "mysql"） */
    private String type;

    /** 主机 */
    private String host;

    /** 端口 */
    private Integer port;

    /** 该数据源关联的库名列表 */
    private List<String> databaseNames;

    /** 连接用户名 */
    private String username;

    /** 已解密的明文密码 */
    private String password;
}
