package com.aipanel.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * User Service Application
 */
@SpringBootApplication(scanBasePackages = "com.aipanel.user")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.aipanel")
@EnableTransactionManagement
@EnableCaching
@EnableAsync
@MapperScan("com.aipanel.user.data.mysql.mapper")
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
