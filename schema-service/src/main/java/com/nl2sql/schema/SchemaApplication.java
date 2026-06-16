package com.nl2sql.schema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class SchemaApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemaApplication.class, args);
    }
}
