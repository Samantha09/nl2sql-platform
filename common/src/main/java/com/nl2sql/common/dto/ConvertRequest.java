package com.nl2sql.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * NL → SQL 转换请求。ai-service 与 query-service 共享，供 Feign 调用与控制器入参统一。
 */
@Data
public class ConvertRequest implements Serializable {

    /** 目标数据源 ID */
    @NotNull(message = "数据源 ID 不能为空")
    private Long dataSourceId;

    /** 自然语言查询内容 */
    @NotBlank(message = "自然语言查询内容不能为空")
    private String naturalLanguage;

    /** 目标库名：缺省时由调用方按数据源唯一库解析，多库时必填 */
    private String databaseName;
}
