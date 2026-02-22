package com.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2璧勬簮鏈嶅姟鍣ㄥ熀纭€閰嶇疆
 * 鎻愪緵閫氱敤鐨凧WT楠岃瘉鍜屾潈闄愰厤缃紝鍑忓皯鍚勬湇鍔＄殑閲嶅浠ｇ爜
 *
 * @author what's up
 * @since 2025-10-05
 */
@Slf4j
public abstract class BaseResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:${AUTH_JWK_SET_URI:http://127.0.0.1:8081/.well-known/jwks.json}}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String issuerUri;

    /**
     * 閰嶇疆瀹夊叏杩囨护鍣ㄩ摼
     * 瀛愮被鍙互瑕嗙洊姝ゆ柟娉曟潵瀹氬埗鑷繁鐨勫畨鍏ㄨ鍒?
     */
    @Bean
    @Order(100)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String serviceName = getServiceName();
        log.info("馃敡 閰嶇疆{}鐨凮Auth2.1璧勬簮鏈嶅姟鍣ㄥ畨鍏ㄨ繃婊ゅ櫒閾?, serviceName);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowCredentials(true);
                    config.addAllowedOriginPattern("*");
                    config.addAllowedHeader("*");
                    config.addAllowedMethod("*");
                    return config;
                }))
                .authorizeHttpRequests(authz -> {
                    // 閰嶇疆鍏叡绔偣
                    configurePublicEndpoints(authz);
                    // 閰嶇疆鏈嶅姟鐗瑰畾鐨勭鐐?
                    configureServiceEndpoints(authz);
                    // 鍏朵粬璇锋眰闇€瑕佽璇?
                    authz.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("馃敀 JWT璁よ瘉澶辫触: {}", authException.getMessage());
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"error\":\"unauthorized\",\"message\":\"JWT浠ょ墝鏃犳晥鎴栧凡杩囨湡\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("馃毇 JWT鎺堟潈澶辫触: {}", accessDeniedException.getMessage());
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"error\":\"access_denied\",\"message\":\"鏉冮檺涓嶈冻\"}"
                            );
                        })
                );

        log.info("鉁?{}OAuth2.1璧勬簮鏈嶅姟鍣ㄥ畨鍏ㄨ繃婊ゅ櫒閾鹃厤缃畬鎴?, serviceName);
        return http.build();
    }

    /**
     * 閰嶇疆鍏叡绔偣锛堟墍鏈夋湇鍔￠€氱敤锛?
     */
    protected void configurePublicEndpoints(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        authz
                // 鍋ュ悍妫€鏌ュ拰鐩戞帶
                .requestMatchers("/actuator/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                // API鏂囨。
                .requestMatchers("/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll();
    }

    /**
     * 閰嶇疆鏈嶅姟鐗瑰畾鐨勭鐐规潈闄?
     * 瀛愮被蹇呴』瀹炵幇姝ゆ柟娉曟潵瀹氫箟鑷繁鐨勭鐐规潈闄?
     */
    protected abstract void configureServiceEndpoints(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz);

    /**
     * 鑾峰彇鏈嶅姟鍚嶇О
     * 鐢ㄤ簬鏃ュ織璁板綍
     */
    protected abstract String getServiceName();

    /**
     * JWT瑙ｇ爜鍣ㄩ厤缃?
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("配置{}JWT解码器，JWK端点: {}, issuer: {}", getServiceName(), jwkSetUri, issuerUri);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        decoder.setJwtValidator(withIssuer);
        return decoder;
    }

    /**
     * JWT璁よ瘉杞崲鍣ㄩ厤缃?
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // OAuth2.1鏍囧噯锛氫粠scope瀛楁涓彁鍙栨潈闄愶紝浣跨敤SCOPE_鍓嶇紑
        authoritiesConverter.setAuthorityPrefix("SCOPE_");
        authoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return converter;
    }
}

