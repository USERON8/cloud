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
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;






@Slf4j
@Component
public class JwtTokenForwardFilter implements GlobalFilter, Ordered {

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
                            .header("Authorization", "Bearer " + token);


                    
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
            addHeaderIfPresent(requestBuilder, "X-User-Type", jwt.getClaimAsString("user_type"));
            addHeaderIfPresent(requestBuilder, "X-User-Id", getClaimAsString(jwt, "user_id"));
            addHeaderIfPresent(requestBuilder, "X-User-Nickname", jwt.getClaimAsString("nickname"));
            addHeaderIfPresent(requestBuilder, "X-User-Status", getClaimAsString(jwt, "status"));

            
            addHeaderIfPresent(requestBuilder, "X-Client-Id", jwt.getClaimAsString("client_id"));

            
            addHeaderIfPresent(requestBuilder, "X-Token-Version", jwt.getClaimAsString("token_version"));

            
            if (jwt.getClaimAsString("scope") != null) {
                addHeaderIfPresent(requestBuilder, "X-User-Scopes", jwt.getClaimAsString("scope"));
            }

            log.debug("Added user claim headers for user {}", jwt.getClaimAsString("username"));

        } catch (Exception e) {
            log.warn("Failed to extract JWT user claims: {}", e.getMessage());
        }
    }

    


    private void addHeaderIfPresent(ServerHttpRequest.Builder requestBuilder, String headerName, String value) {
        if (StringUtils.hasText(value) && !"null".equals(value)) {
            requestBuilder.header(headerName, value);
        }
    }

    


    private String getClaimAsString(Jwt jwt, String claimName) {
        Object claim = jwt.getClaim(claimName);
        return claim != null ? claim.toString() : null;
    }

    @Override
    public int getOrder() {
        return -100; 
    }
}
