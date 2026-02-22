package com.cloud.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 鐢ㄦ埛鏈嶅姟 OAuth2.1璧勬簮鏈嶅姟鍣ㄩ厤缃?
 * 鐙珛鐨凮Auth2璧勬簮鏈嶅姟鍣ㄩ厤缃紝涓嶄緷璧朿ommon-module
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ResourceServerConfig {

    private final TokenBlacklistChecker tokenBlacklistChecker;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:${AUTH_JWK_SET_URI:http://127.0.0.1:8081/.well-known/jwks.json}}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String issuerUri;

    /**
     * 閰嶇疆鐢ㄦ埛鏈嶅姟鐨勫畨鍏ㄨ繃婊ゅ櫒閾?
     */
    @Bean
    @Order(100)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("馃敡 閰嶇疆鐢ㄦ埛鏈嶅姟OAuth2.1璧勬簮鏈嶅姟鍣ㄥ畨鍏ㄨ繃婊ゅ櫒閾?);

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
                .authorizeHttpRequests(authz -> authz
                        // 鍏叡绔偣鏀捐
                        .requestMatchers("/actuator/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll()

                        // 鍐呴儴API闇€瑕乮nternal_api scope
                        .requestMatchers("/user/internal/**")
                        .hasAuthority("SCOPE_internal_api")

                        // 绠＄悊鍛樼鐞嗘帴鍙?- 闇€瑕佺鐞嗗憳鏉冮檺
                        .requestMatchers("/admin/manage/**")
                        .hasAuthority("SCOPE_admin")

                        // 鐢ㄦ埛绠＄悊鎺ュ彛 - 闇€瑕佺鐞嗘潈闄?
                        .requestMatchers("/user/manage/**")
                        .hasAuthority("SCOPE_admin")

                        // 鐢ㄦ埛璧勬枡鎺ュ彛 - 鐢ㄦ埛鍙互璁块棶鑷繁鐨勬垨绠＄悊鍛樺彲浠ヨ闂换浣曠敤鎴风殑
                        .requestMatchers("/api/user/profile/**")
                        .hasAnyAuthority("SCOPE_user", "SCOPE_admin")

                        // 鐢ㄦ埛鍦板潃鎺ュ彛 - 鐢ㄦ埛鏉冮檺
                        .requestMatchers("/api/user/address/**")
                        .hasAnyAuthority("SCOPE_user", "SCOPE_admin")

                        // 鍟嗘埛鐩稿叧鎺ュ彛 - 鍟嗘埛鏉冮檺鎴栫鐞嗗憳鏉冮檺
                        .requestMatchers("/api/user/merchant/**")
                        .hasAnyAuthority("SCOPE_merchant", "SCOPE_admin")

                        // 鏉冮檺绀轰緥鎺ュ彛闇€瑕佽璇?
                        .requestMatchers("/example/permissions/**").authenticated()

                        // 鍏朵粬璇锋眰闇€瑕佽璇?
                        .anyRequest().authenticated()
                )
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

        log.info("鉁?鐢ㄦ埛鏈嶅姟OAuth2.1璧勬簮鏈嶅姟鍣ㄥ畨鍏ㄨ繃婊ゅ櫒閾鹃厤缃畬鎴?);
        return http.build();
    }

    /**
     * JWT瑙ｇ爜鍣ㄩ厤缃?
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("配置用户服务JWT解码器，JWK端点: {}, issuer: {}", jwkSetUri, issuerUri);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, tokenBlacklistChecker));
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


