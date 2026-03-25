package com.dustopus.infinitechat.moment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.dustopus.infinitechat.moment.mapper")
public class MomentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MomentServiceApplication.class, args);
    }
}
