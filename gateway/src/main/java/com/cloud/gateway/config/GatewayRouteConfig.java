package com.cloud.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("code-oauth2-endpoints", r -> r
                        .path("/oauth2/**")
                        .filters(f -> f.preserveHostHeader())
                        .uri("lb://auth-service"))
                .route("code-oidc-endpoints", r -> r
                        .path("/.well-known/**", "/connect/**", "/userinfo")
                        .filters(f -> f.preserveHostHeader())
                        .uri("lb://auth-service"))
                .route("code-auth-service-api", r -> r
                        .path("/auth/**")
                        .uri("lb://auth-service"))
                .route("code-user-service-api", r -> r
                        .path(
                                "/api/manage/users/**",
                                "/api/query/users/**",
                                "/api/user/address/**",
                                "/api/user/profile/**",
                                "/api/user/notification/**",
                                "/api/merchant/**",
                                "/api/admin/**",
                                "/api/statistics/**",
                                "/api/thread-pool/**"
                        )
                        .uri("lb://user-service"))
                .route("code-product-service-api", r -> r
                        .path("/api/product", "/api/product/**", "/api/category", "/api/category/**")
                        .uri("lb://product-service"))
                .route("code-order-service-api", r -> r
                        .path("/api/orders/**", "/api/v1/refund/**")
                        .uri("lb://order-service"))
                .route("code-payment-service-api", r -> r
                        .path("/api/payments/**", "/api/v1/payment/alipay/**")
                        .uri("lb://payment-service"))
                .route("code-stock-service-api", r -> r
                        .path("/api/stocks/**")
                        .uri("lb://stock-service"))
                .route("code-search-service-api", r -> r
                        .path("/api/search", "/api/search/**")
                        .uri("lb://search-service"))
                .route("code-auth-service-doc", r -> r
                        .path("/auth-service/doc.html", "/auth-service/webjars/**", "/auth-service/swagger-ui/**", "/auth-service/v3/api-docs/**", "/auth-service/swagger-resources/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://auth-service"))
                .route("code-user-service-doc", r -> r
                        .path("/user-service/doc.html", "/user-service/webjars/**", "/user-service/swagger-ui/**", "/user-service/v3/api-docs/**", "/user-service/swagger-resources/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://user-service"))
                .route("code-product-service-doc", r -> r
                        .path("/product-service/doc.html", "/product-service/webjars/**", "/product-service/swagger-ui/**", "/product-service/v3/api-docs/**", "/product-service/swagger-resources/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://product-service"))
                .route("code-order-service-doc", r -> r
                        .path("/order-service/doc.html", "/order-service/webjars/**", "/order-service/swagger-ui/**", "/order-service/v3/api-docs/**", "/order-service/swagger-resources/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://order-service"))
                .route("code-payment-service-doc", r -> r
                        .path("/payment-service/doc.html", "/payment-service/webjars/**", "/payment-service/swagger-ui/**", "/payment-service/v3/api-docs/**", "/payment-service/swagger-resources/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://payment-service"))
                .route("code-stock-service-doc", r -> r
                        .path("/stock-service/doc.html", "/stock-service/webjars/**", "/stock-service/swagger-ui/**", "/stock-service/v3/api-docs/**", "/stock-service/swagger-resources/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://stock-service"))
                .route("code-search-service-doc", r -> r
                        .path("/search-service/doc.html", "/search-service/webjars/**", "/search-service/swagger-ui/**", "/search-service/v3/api-docs/**", "/search-service/swagger-resources/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://search-service"))
                .build();
    }
}
