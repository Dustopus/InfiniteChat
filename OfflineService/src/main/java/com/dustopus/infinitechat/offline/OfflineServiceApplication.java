package com.dustopus.infinitechat.offline;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.dustopus.infinitechat.offline.mapper")
public class OfflineServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OfflineServiceApplication.class, args);
    }
}
