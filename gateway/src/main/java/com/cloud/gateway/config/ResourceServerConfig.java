package com.cloud.gateway.config;

import lombok.RequiredArgsConstructor;`r`nimport lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;`r`nimport org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;`r`nimport org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * OAuth2璧勬簮鏈嶅姟鍣ㄩ厤缃?
 * 鎻愪緵JWT token楠岃瘉鍜屾潈闄愰厤缃?
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity`r`n@RequiredArgsConstructor
public class ResourceServerConfig {

    private static final String BLACKLIST_KEY_PREFIX = "oauth2:blacklist:";

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String issuerUri;

    @Value("${app.security.enable-test-api:false}")
    private boolean enableTestApi;

    /**
     * 閰嶇疆瀹夊叏杩囨护鍣ㄩ摼
     *
     * @param http ServerHttpSecurity瀵硅薄
     * @return SecurityWebFilterChain 瀹夊叏杩囨护鍣ㄩ摼
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        log.info("馃敡 閰嶇疆缃戝叧瀹夊叏杩囨护鍣ㄩ摼锛屾祴璇旳PI寮€鏀剧姸鎬? {}", enableTestApi);

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> {
                    var authExchanges = exchanges
                            // ========== OAuth2.1鏍囧噯绔偣 - 瀹屽叏寮€鏀?==========
                            .pathMatchers("/oauth2/**", "/.well-known/**", "/userinfo").permitAll()
                            .pathMatchers("/connect/**").permitAll()  // OpenID Connect绔偣

                            // ========== 璁よ瘉鏈嶅姟鍏紑API - 鏃犻渶token ==========
                            // 璁よ瘉鏈嶅姟绔偣
                            .pathMatchers(HttpMethod.POST,
                                    "/auth/register",
                                    "/auth/login",
                                    "/auth/users/register",
                                    "/auth/sessions",
                                    "/auth/users/register-and-login",
                                    "/auth/tokens/refresh",
                                    "/auth/register-and-login",
                                    "/auth/refresh-token"
                            ).permitAll()
                            .pathMatchers("/auth/oauth2/github/**", "/auth/github/**").permitAll()

                            // 鏈嶅姟鍓嶇紑璺緞锛堝吋瀹规€э級

                            // 閫氱敤璁よ瘉璺緞
                            .pathMatchers("/login/**", "/register/**", "/logout/**").permitAll()

                            // ========== 鍋ュ悍妫€鏌ュ拰鐩戞帶 ==========
                            .pathMatchers("/actuator/**", "/health/**", "/metrics/**").permitAll()

                            // ========== Knife4j鍜孉PI鏂囨。 - 瀹屽叏寮€鏀?==========
                            // Knife4j鏍稿績璺緞
                            .pathMatchers(
                                    "/doc.html",
                                    "/doc.html/**",
                                    "/**/doc.html",  // 鍖归厤鎵€鏈夋湇鍔＄殑doc.html
                                    "/**/doc.html/**"
                            ).permitAll()

                            // Swagger UI
                            .pathMatchers(
                                    "/swagger-ui.html",
                                    "/swagger-ui/**",
                                    "/**/swagger-ui/**"
                            ).permitAll()

                            // API鏂囨。璧勬簮
                            .pathMatchers(
                                    "/v3/api-docs/**",
                                    "/**/v3/api-docs/**",
                                    "/swagger-resources/**",
                                    "/**/swagger-resources/**",
                                    "/webjars/**",
                                    "/**/webjars/**"
                            ).permitAll()

                            // 闈欐€佽祫婧?
                            .pathMatchers(
                                    "/favicon.ico",
                                    "/csrf",
                                    "/error",
                                    "/static/**",
                                    "/public/**"
                            ).permitAll()

                            // 鍚勫井鏈嶅姟鐨勬枃妗ｇ鐐癸紙鏄庣‘鍒楀嚭锛?
                            .pathMatchers(
                                    "/auth-service/doc.html", "/auth-service/doc.html/**",
                                    "/user-service/doc.html", "/user-service/doc.html/**",
                                    "/product-service/doc.html", "/product-service/doc.html/**",
                                    "/order-service/doc.html", "/order-service/doc.html/**",
                                    "/payment-service/doc.html", "/payment-service/doc.html/**",
                                    "/stock-service/doc.html", "/stock-service/doc.html/**",
                                    "/search-service/doc.html", "/search-service/doc.html/**",
                                    "/log-service/doc.html", "/log-service/doc.html/**"
                            ).permitAll();

                    // 鏍规嵁閰嶇疆鍐冲畾鏄惁寮€鏀炬祴璇旳PI
                    if (enableTestApi) {
                        log.warn("鈿狅笍 娴嬭瘯API宸插紑鏀撅紝鐢熶骇鐜璇峰叧闂閰嶇疆");
                        authExchanges = authExchanges.pathMatchers("/test/**").permitAll();
                    }

                    // 闇€瑕佽璇佺殑涓氬姟绔偣
                    authExchanges
                            // 鐢ㄦ埛鏈嶅姟 - 闇€瑕佽璇?
                            .pathMatchers("/users/**", "/merchant/**", "/admin/**").authenticated()
                            // 鍟嗗搧鏈嶅姟 - 閮ㄥ垎鍏紑锛堟祻瑙堬級锛岄儴鍒嗛渶瑕佽璇侊紙绠＄悊锛?
                            .pathMatchers("/product/admin/**", "/category/admin/**").authenticated()
                            .pathMatchers("/product/**", "/category/**").permitAll()  // 鍟嗗搧娴忚鍏紑
                            // 璁㈠崟鏈嶅姟 - 闇€瑕佽璇?
                            .pathMatchers("/order/**", "/cart/**").authenticated()
                            // 鏀粯鏈嶅姟 - 闇€瑕佽璇?
                            .pathMatchers("/payment/**").authenticated()
                            // 搴撳瓨鏈嶅姟 - 闇€瑕佽璇?
                            .pathMatchers("/stock/**").authenticated()
                            // 鎼滅储鏈嶅姟 - 鍏紑
                            .pathMatchers("/search/**").permitAll()
                            // 鏃ュ織鏈嶅姟 - 闇€瑕佽璇?
                            .pathMatchers("/log/**").authenticated()
                            // 鍏朵粬鎵€鏈夎姹傞兘闇€瑕佽璇?
                            .anyExchange().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder())
                        )
                        .authenticationEntryPoint((exchange, ex) -> {
                            log.warn("OAuth2璁よ瘉澶辫触: {}", ex.getMessage());
                            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                            exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
                            String jsonResponse = "{\"code\":401,\"error\":\"Unauthorized\",\"message\":\"璁よ瘉澶辫触锛岃鎻愪緵鏈夋晥鐨凧WT Token\",\"timestamp\":" + System.currentTimeMillis() + "}";
                            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                                    .bufferFactory().wrap(jsonResponse.getBytes())));
                        })
                        .accessDeniedHandler((exchange, ex) -> {
                            log.warn("OAuth2鏉冮檺涓嶈冻: {}", ex.getMessage());
                            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
                            exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
                            String jsonResponse = "{\"code\":403,\"error\":\"Forbidden\",\"message\":\"鏉冮檺涓嶈冻锛屾棤娉曡闂璧勬簮\",\"timestamp\":" + System.currentTimeMillis() + "}";
                            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                                    .bufferFactory().wrap(jsonResponse.getBytes())));
                        })
                );

        return http.build();
    }

    /**
     * JWT瑙ｇ爜鍣?
     * 浣跨敤璁よ瘉鏈嶅姟鐨凧WK绔偣杩涜JWT楠岃瘉
     *
     * @return ReactiveJwtDecoder JWT瑙ｇ爜鍣ㄥ疄渚?
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        log.info("配置JWT解码器，JWK端点: {}, issuer: {}", jwkSetUri, issuerUri);
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer));
        return token -> decoder.decode(token)
                .flatMap(jwt -> reactiveStringRedisTemplate.hasKey(BLACKLIST_KEY_PREFIX + extractTokenId(jwt, token))
                        .flatMap(blacklisted -> {
                            if (Boolean.TRUE.equals(blacklisted)) {
                                return Mono.error(new BadJwtException("Token is blacklisted"));
                            }
                            return Mono.just(jwt);
                        })
                        .onErrorResume(ex -> {
                            // Keep gateway available when Redis is transiently unavailable.
                            log.error("gateway jwt blacklist validation failed", ex);
                            return Mono.just(jwt);
                        }));
    }

    private String extractTokenId(Jwt jwt, String tokenValue) {
        String jti = jwt.getClaimAsString("jti");
        if (jti != null && !jti.isBlank()) {
            return jti;
        }
        return String.valueOf(tokenValue.hashCode());
    }
}



