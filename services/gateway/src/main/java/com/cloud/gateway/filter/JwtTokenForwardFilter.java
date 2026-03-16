package com.cloud.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import cn.hutool.core.util.StrUtil;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;






@Slf4j
@Component
public class JwtTokenForwardFilter implements GlobalFilter, Ordered {

    private static final String[] FORWARDED_IDENTITY_HEADERS = {
            "X-User-Name",
            "X-User-Id",
            "X-User-Nickname",
            "X-User-Status",
            "X-Client-Id",
            "X-User-Scopes",
            "X-User-Roles",
            "X-Auth-Token"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(jwtAuth -> {
                    Jwt jwt = jwtAuth.getToken();
                    String token = jwt.getTokenValue();

                    log.debug("Forward JWT token and user claims to downstream services");


                    ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                            .headers(headers -> {
                                for (String header : FORWARDED_IDENTITY_HEADERS) {
                                    headers.remove(header);
                                }
                                headers.set("Authorization", "Bearer " + token);
                                headers.set("X-Auth-Token", token);
                            });



                    addUserInfoHeaders(requestBuilder, jwt);

                    ServerHttpRequest request = requestBuilder.build();

                    log.debug("Successfully forwarded token and user claims for user {}", jwtAuth.getName());
                    return exchange.mutate().request(request).build();
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }




    private void addUserInfoHeaders(ServerHttpRequest.Builder requestBuilder, Jwt jwt) {
        try {

            addHeaderIfPresent(requestBuilder, "X-User-Name", jwt.getClaimAsString("username"));
            addHeaderIfPresent(requestBuilder, "X-User-Id", getUserIdClaim(jwt));
            addHeaderIfPresent(requestBuilder, "X-User-Nickname", jwt.getClaimAsString("nickname"));
            addHeaderIfPresent(requestBuilder, "X-User-Status", getClaimAsString(jwt, "status"));


            addHeaderIfPresent(requestBuilder, "X-Client-Id", jwt.getClaimAsString("client_id"));


            if (jwt.getClaimAsString("scope") != null) {
                addHeaderIfPresent(requestBuilder, "X-User-Scopes", jwt.getClaimAsString("scope"));
            }

            Object roles = jwt.getClaim("roles");
            if (roles instanceof java.util.Collection<?> roleCollection && !roleCollection.isEmpty()) {
                addHeaderIfPresent(requestBuilder, "X-User-Roles",
                        roleCollection.stream().map(Object::toString).collect(java.util.stream.Collectors.joining(" ")));
            }

            log.debug("Added user claim headers for user {}", jwt.getClaimAsString("username"));

        } catch (Exception e) {
            log.warn("Failed to extract JWT user claims: {}", e.getMessage());
        }
    }




    private void addHeaderIfPresent(ServerHttpRequest.Builder requestBuilder, String headerName, String value) {
        if (StrUtil.isNotBlank(value) && !"null".equals(value)) {
            requestBuilder.header(headerName, value);
        }
    }




    private String getClaimAsString(Jwt jwt, String claimName) {
        Object claim = jwt.getClaim(claimName);
        return claim != null ? claim.toString() : null;
    }

    private String getUserIdClaim(Jwt jwt) {
        String userId = getClaimAsString(jwt, "user_id");
        if (StrUtil.isBlank(userId)) {
            userId = getClaimAsString(jwt, "userId");
        }
        return userId;
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

