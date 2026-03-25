package com.dustopus.infinitechat.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/v1/user/**")
                        .uri("lb://AuthenticationService"))
                .route("contact-service", r -> r.path("/api/v1/contact/**", "/api/v1/group/**")
                        .uri("lb://ContactService"))
                .route("messaging-service", r -> r.path("/api/v1/message/**", "/api/v1/redpacket/**")
                        .uri("lb://MessagingService"))
                .route("moment-service", r -> r.path("/api/v1/moment/**")
                        .uri("lb://MomentService"))
                .route("notify-service", r -> r.path("/api/v1/notify/**")
                        .uri("lb://NotifyService"))
                .route("offline-service", r -> r.path("/api/v1/offline/**")
                        .uri("lb://OfflineService"))
                .route("upload-service", r -> r.path("/api/v1/upload/**")
                        .uri("lb://MomentService"))
                .build();
    }
}
