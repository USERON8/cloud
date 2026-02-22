package com.cloud.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 瀹夊叏杩囨护鍣ㄩ摼閰嶇疆
 * 涓ユ牸閬靛惊OAuth2.1鏍囧噯锛屽垎绂绘巿鏉冩湇鍔″櫒鍜岃祫婧愭湇鍔″櫒鐨勫畨鍏ㄩ厤缃?
 * <p>
 * 閰嶇疆浼樺厛绾?
 * 1. OAuth2鎺堟潈鏈嶅姟鍣ㄨ繃婊ゅ櫒閾?(Order = 1)
 * 2. 璧勬簮鏈嶅姟鍣ㄨ繃婊ゅ櫒閾?(Order = 2)
 * 3. 榛樿杩囨护鍣ㄩ摼 (Order = 3)
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class SecurityFilterChainConfig {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public SecurityFilterChainConfig(JwtDecoder jwtDecoder,
                                     @Qualifier("enhancedJwtAuthenticationConverter") JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    /**
     * OAuth2.1鎺堟潈鏈嶅姟鍣ㄥ畨鍏ㄨ繃婊ゅ櫒閾?
     * 澶勭悊OAuth2鎺堟潈鏈嶅姟鍣ㄧ殑鎵€鏈夌鐐?
     * <p>
     * 澶勭悊鐨勭鐐?
     * - /oauth2/authorize (鎺堟潈绔偣)
     * - /oauth2/token (浠ょ墝绔偣)
     * - /oauth2/revoke (浠ょ墝鎾ら攢绔偣)
     * - /oauth2/introspect (浠ょ墝鍐呯渷绔偣)
     * - /oauth2/jwks (JWK闆嗗悎绔偣)
     * - /.well-known/oauth-authorization-server (鍙戠幇绔偣)
     * - /connect/logout (OpenID Connect鐧诲嚭绔偣)
     * - /userinfo (鐢ㄦ埛淇℃伅绔偣)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("馃敡 閰嶇疆OAuth2.1鎺堟潈鏈嶅姟鍣ㄥ畨鍏ㄨ繃婊ゅ櫒閾?);

        // 鍒涘缓OAuth2鎺堟潈鏈嶅姟鍣ㄩ厤缃櫒
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        // 鍚敤OpenID Connect鏀寔
        authorizationServerConfigurer.oidc(Customizer.withDefaults());

        http
                // 鍖归厤OAuth2鍜孫penID Connect鐩稿叧绔偣
                .securityMatcher(new RequestMatcher() {
                    @Override
                    public boolean matches(jakarta.servlet.http.HttpServletRequest request) {
                        String path = request.getRequestURI();
                        return path.startsWith("/oauth2/") ||
                                path.startsWith("/.well-known/") ||
                                path.startsWith("/connect/") ||
                                "/userinfo".equals(path);
                    }
                })

                // 搴旂敤OAuth2鎺堟潈鏈嶅姟鍣ㄩ厤缃?
                .with(authorizationServerConfigurer, Customizer.withDefaults())

                // OAuth2.1瀹夊叏閰嶇疆
                .csrf(AbstractHttpConfigurer::disable)  // OAuth2涓嶉渶瑕丆SRF淇濇姢
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowCredentials(true);
                    config.addAllowedOriginPattern("*");
                    config.addAllowedHeader("*");
                    config.addAllowedMethod("*");
                    return config;
                }))

                // 鎺堟潈閰嶇疆 - OAuth2鎺堟潈鏈嶅姟鍣ㄤ細鑷姩澶勭悊绔偣鎺堟潈
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )

                // OAuth2.1璁よ瘉鏂瑰紡
                .httpBasic(Customizer.withDefaults())  // 鏀寔HTTP Basic璁よ瘉
                .formLogin(Customizer.withDefaults())  // 鏀寔琛ㄥ崟鐧诲綍锛堢敤浜庢巿鏉冮〉闈級

                // OAuth2.1浼氳瘽绠＄悊
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)  // 鎺堟潈鏈嶅姟鍣ㄩ渶瑕佷細璇濇敮鎸?
                        .maximumSessions(1)  // 闄愬埗骞跺彂浼氳瘽
                        .maxSessionsPreventsLogin(false)  // 鍏佽韪㈠嚭鏃т細璇?
                );

        log.info("鉁?OAuth2.1鎺堟潈鏈嶅姟鍣ㄥ畨鍏ㄨ繃婊ゅ櫒閾鹃厤缃畬鎴?);
        return http.build();
    }

    /**
     * OAuth2.1璧勬簮鏈嶅姟鍣ㄥ畨鍏ㄨ繃婊ゅ櫒閾?
     * 澶勭悊鍙椾繚鎶ょ殑API绔偣锛岄獙璇丣WT浠ょ墝
     * <p>
     * 澶勭悊鐨勭鐐?
     * - /auth/** (璁よ瘉鐩稿叧API)
     * - /admin/** (绠＄悊API)
     * - 鍏朵粬闇€瑕丣WT楠岃瘉鐨凙PI
     */
    @Bean
    @Order(2)
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("馃敡 閰嶇疆OAuth2.1璧勬簮鏈嶅姟鍣ㄥ畨鍏ㄨ繃婊ゅ櫒閾?);

        http
                // 鍖归厤闇€瑕丣WT楠岃瘉鐨勭鐐?
                .securityMatcher(
                        "/auth/**",
                        "/admin/**",
                        "/management/**"
                )

                // OAuth2.1璧勬簮鏈嶅姟鍣ㄩ厤缃?
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                // 鎺堟潈閰嶇疆
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/users/register",
                                "/auth/sessions",
                                "/auth/users/register-and-login",
                                "/auth/tokens/refresh",
                                // backward-compatible paths
                                "/auth/register",
                                "/auth/login",
                                "/auth/register-and-login",
                                "/auth/refresh-token"
                        ).permitAll()
                        .requestMatchers("/auth/oauth2/github/**").permitAll()
                        .requestMatchers("/admin/**", "/management/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                // OAuth2.1璧勬簮鏈嶅姟鍣↗WT閰嶇疆
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )

                        // JWT璁よ瘉寮傚父澶勭悊
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("馃敀 JWT璁よ瘉澶辫触: {}", authException.getMessage());
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"error\":\"unauthorized\",\"message\":\"JWT浠ょ墝鏃犳晥鎴栧凡杩囨湡\"}"
                            );
                        })

                        // JWT鎺堟潈寮傚父澶勭悊
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("馃毇 JWT鎺堟潈澶辫触: {}", accessDeniedException.getMessage());
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"error\":\"access_denied\",\"message\":\"鏉冮檺涓嶈冻\"}"
                            );
                        })
                )

                // 鏃犵姸鎬佷細璇?
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        log.info("鉁?OAuth2.1璧勬簮鏈嶅姟鍣ㄥ畨鍏ㄨ繃婊ゅ櫒閾鹃厤缃畬鎴?);
        return http.build();
    }

    /**
     * 榛樿瀹夊叏杩囨护鍣ㄩ摼
     * 澶勭悊鍏朵粬鎵€鏈夎姹傦紝鍖呮嫭鍏紑API鍜屾枃妗ｇ鐐?
     * <p>
     * 澶勭悊鐨勭鐐?
     * - 鍏紑API (濡傛敞鍐屻€佺櫥褰曠瓑)
     * - 鏂囨。绔偣 (Swagger, Actuator绛?
     * - 闈欐€佽祫婧?
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("馃敡 閰嶇疆榛樿瀹夊叏杩囨护鍣ㄩ摼");

        http
                // OAuth2.1鍩虹閰嶇疆
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                // 鎺堟潈閰嶇疆
                .authorizeHttpRequests(authorize -> authorize
                        // 瀹屽叏鍏紑鐨勭鐐?
                        .requestMatchers(
                                // 鍋ュ悍妫€鏌ュ拰鐩戞帶
                                "/actuator/**",
                                "/health/**",

                                // API鏂囨。 - Swagger/OpenAPI
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/webjars/**",

                                // Knife4j鏂囨。 - 鍏紑璁块棶
                                "/doc.html",              // Knife4j鏂囨。棣栭〉
                                "/doc.html/**",           // Knife4j鐩稿叧璧勬簮
                                "/favicon.ico",
                                "/error",

                                // 绠€鍗曠櫥褰曢〉闈?- 鍏紑璁块棶
                                "/login",                 // 绠€鍗曠櫥褰曢〉闈?
                                "/login/**",              // 鐧诲綍鐩稿叧璧勬簮

                                // 鍏紑API
                                "/auth/register",         // 鐢ㄦ埛娉ㄥ唽
                                "/auth/login",            // 鐢ㄦ埛鐧诲綍
                                "/auth/logout",           // 鐢ㄦ埛鐧诲嚭
                                "/auth/register-and-login", // 娉ㄥ唽骞剁櫥褰?
                                "/auth/refresh-token",    // 鍒锋柊浠ょ墝
                                "/auth/github/**"         // GitHub OAuth2
                        ).permitAll()

                        // 鍏朵粬璇锋眰鍏佽璁块棶锛堢敱缃戝叧缁熶竴閴存潈锛?
                        .anyRequest().permitAll()
                )

                // 绂佺敤涓嶉渶瑕佺殑璁よ瘉鏂瑰紡
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // 鏃犵姸鎬佷細璇?
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        log.info("鉁?榛樿瀹夊叏杩囨护鍣ㄩ摼閰嶇疆瀹屾垚");
        return http.build();
    }

    /**
     * CORS閰嶇疆锛堝叏灞€锛?
     * OAuth2.1鏍囧噯鎺ㄨ崘鐨勮法鍩熼厤缃?
     */
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        log.info("馃敡 閰嶇疆CORS璺ㄥ煙璁剧疆");

        org.springframework.web.cors.CorsConfiguration configuration =
                new org.springframework.web.cors.CorsConfiguration();

        // OAuth2.1 CORS璁剧疆
        configuration.setAllowCredentials(true);
        configuration.addAllowedOriginPattern("http://localhost:*");
        configuration.addAllowedOriginPattern("https://localhost:*");
        configuration.addAllowedOriginPattern("http://127.0.0.1:*");
        configuration.addAllowedOriginPattern("https://127.0.0.1:*");

        // 鍏佽鐨凥TTP鏂规硶
        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("OPTIONS");

        // 鍏佽鐨勮姹傚ご
        configuration.addAllowedHeader("*");

        // 鏆撮湶鐨勫搷搴斿ご锛圤Auth2.1闇€瑕侊級
        configuration.addExposedHeader("Authorization");
        configuration.addExposedHeader("Cache-Control");
        configuration.addExposedHeader("Content-Type");

        // 棰勬璇锋眰缂撳瓨鏃堕棿
        configuration.setMaxAge(3600L);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("鉁?CORS璺ㄥ煙璁剧疆閰嶇疆瀹屾垚");
        return source;
    }

}


