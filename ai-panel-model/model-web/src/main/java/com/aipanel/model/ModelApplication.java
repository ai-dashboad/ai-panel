package com.aipanel.model;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * AI Panel Model Service Application
 */
@SpringBootApplication(scanBasePackages = "com.aipanel")
@EnableDiscoveryClient
@MapperScan("com.aipanel.model.data.mapper")
public class ModelApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModelApplication.class, args);
    }
}
