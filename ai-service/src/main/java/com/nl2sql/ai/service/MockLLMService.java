package com.nl2sql.ai.service;

import org.springframework.stereotype.Service;

@Service
public class MockLLMService {

    public String convert(String naturalLanguage, Long dataSourceId) {
        String lower = naturalLanguage.toLowerCase();
        if (lower.contains("销售") || lower.contains("sale")) {
            return "SELECT product_name, SUM(amount) AS total_sales FROM orders GROUP BY product_name ORDER BY total_sales DESC LIMIT 10;";
        }
        if (lower.contains("用户") || lower.contains("user")) {
            return "SELECT COUNT(*) AS user_count FROM users;";
        }
        return "SELECT * FROM orders LIMIT 100;";
    }
}
