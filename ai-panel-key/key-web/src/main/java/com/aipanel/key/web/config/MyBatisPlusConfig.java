package com.aipanel.key.web.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus configuration
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * Configure pagination plugin
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // Add pagination plugin for MySQL
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // Set max limit per page to prevent malicious queries
        paginationInnerInterceptor.setMaxLimit(500L);
        // Enable count query optimization
        paginationInnerInterceptor.setOptimizeJoin(true);

        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        return interceptor;
    }
}