package com.aipanel.billing;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI Panel Billing Service Application
 */
@SpringBootApplication(scanBasePackages = "com.aipanel")
@EnableDiscoveryClient
@EnableAsync
@MapperScan("com.aipanel.billing.data.mapper")
public class BillingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingApplication.class, args);
    }
}
