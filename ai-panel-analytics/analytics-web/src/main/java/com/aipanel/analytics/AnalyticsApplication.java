package com.aipanel.analytics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Analytics service application
 */
@SpringBootApplication(scanBasePackages = "com.aipanel.analytics")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.aipanel")
@MapperScan("com.aipanel.analytics.data.mysql.mapper")
@EnableScheduling
public class AnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsApplication.class, args);
    }
}
