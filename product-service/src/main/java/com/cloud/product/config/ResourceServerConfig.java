package com.cloud.product.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 鍟嗗搧鏈嶅姟 OAuth2.1璧勬簮鏈嶅姟鍣ㄩ厤缃?
 * 鐙珛鐨凮Auth2璧勬簮鏈嶅姟鍣ㄩ厤缃紝涓嶄緷璧朿ommon-module
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:${AUTH_JWK_SET_URI:http://127.0.0.1:8081/.well-known/jwks.json}}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String issuerUri;

    /**
     * 閰嶇疆鍟嗗搧鏈嶅姟鐨勫畨鍏ㄨ繃婊ゅ櫒閾?
     */
    @Bean
    @Order(100)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("馃敡 閰嶇疆鍟嗗搧鏈嶅姟OAuth2.1璧勬簮鏈嶅姟鍣ㄥ畨鍏ㄨ繃婊ゅ櫒閾?);

        http
                .csrf(csrf -> csrf.disable())
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
                        .requestMatchers("/product/internal/**")
                        .hasAuthority("SCOPE_internal_api")

                        // 鍟嗗搧绠＄悊鎺ュ彛 - 闇€瑕佸晢鍝佺鐞嗘潈闄愭垨绠＄悊鍛樻潈闄?
                        .requestMatchers("/product/manage/**")
                        .hasAnyAuthority("SCOPE_write", "ROLE_ADMIN")

                        // 鍟嗗搧鏌ヨ鎺ュ彛 - 鍏紑鍙闂紝浣嗛渶瑕佽璇?
                        .requestMatchers("/product/query/**")
                        .authenticated()

                        // API璺緞閰嶇疆 - 闇€瑕佸晢鍝佺浉鍏虫潈闄?
                        .requestMatchers("/api/product/**")
                        .hasAnyAuthority("SCOPE_read", "SCOPE_write", "ROLE_USER", "ROLE_ADMIN")

                        // 鍏朵粬鍟嗗搧鎺ュ彛 - 鍏紑鍙闂紙闇€瑕佽璇侊級
                        .requestMatchers("/product/**")
                        .authenticated()

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

        log.info("鉁?鍟嗗搧鏈嶅姟OAuth2.1璧勬簮鏈嶅姟鍣ㄥ畨鍏ㄨ繃婊ゅ櫒閾鹃厤缃畬鎴?);
        return http.build();
    }

    /**
     * JWT瑙ｇ爜鍣ㄩ厤缃?
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("配置JWT解码器，JWK端点: {}, issuer: {}", jwkSetUri, issuerUri);
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


