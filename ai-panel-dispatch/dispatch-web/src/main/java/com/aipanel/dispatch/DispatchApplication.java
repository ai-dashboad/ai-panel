package com.aipanel.dispatch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Panel Dispatch Service Application
 * Core model routing and AI provider management
 */
@SpringBootApplication(scanBasePackages = "com.aipanel")
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
@MapperScan("com.aipanel.dispatch.data.mapper")
public class DispatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(DispatchApplication.class, args);
    }
}
