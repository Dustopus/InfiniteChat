package com.dustopus.infinitechat.messaging;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.dustopus.infinitechat.messaging.mapper")
public class MessagingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessagingServiceApplication.class, args);
    }
}
