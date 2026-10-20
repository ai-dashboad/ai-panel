package com.aipanel.key;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * API Key Service Application
 */
@SpringBootApplication(scanBasePackages = "com.aipanel.key")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.aipanel")
@EnableTransactionManagement
@EnableCaching
@MapperScan("com.aipanel.key.data.mysql.mapper")
public class KeyApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeyApplication.class, args);
    }
}
