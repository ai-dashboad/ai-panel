package com.aipanel.channel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Channel service application
 *
 * Note: Spring AI auto-configurations are conditionally enabled via application.yml
 * Set OPENAI_API_KEY or ANTHROPIC_API_KEY environment variables to enable providers
 */
@SpringBootApplication(
    scanBasePackages = "com.aipanel.channel",
    exclude = {
        org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration.class,
        org.springframework.ai.autoconfigure.anthropic.AnthropicAutoConfiguration.class
    }
)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.aipanel")
@MapperScan("com.aipanel.channel.data.mysql.mapper")
public class ChannelApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChannelApplication.class, args);
    }
}
