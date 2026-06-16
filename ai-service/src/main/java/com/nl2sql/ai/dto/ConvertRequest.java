package com.nl2sql.ai.dto;

import lombok.Data;

@Data
public class ConvertRequest {
    private Long dataSourceId;
    private String naturalLanguage;
}
