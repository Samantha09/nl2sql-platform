package com.nl2sql.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConvertRequest {

    @NotNull(message = "数据源 ID 不能为空")
    private Long dataSourceId;

    @NotBlank(message = "自然语言查询内容不能为空")
    private String naturalLanguage;
}
