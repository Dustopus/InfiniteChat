package com.dustopus.infinitechat.rtc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class RtcServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RtcServiceApplication.class, args);
    }
}
