package com.aipanel.channel.web.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the admin JWT guard for provider / channel management APIs.
 */
@Configuration
@RequiredArgsConstructor
public class ChannelWebMvcConfig implements WebMvcConfigurer {

    private final AdminJwtInterceptor adminJwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminJwtInterceptor)
            .addPathPatterns("/api/v1/providers", "/api/v1/providers/**",
                "/api/v1/channels", "/api/v1/channels/**");
    }
}
