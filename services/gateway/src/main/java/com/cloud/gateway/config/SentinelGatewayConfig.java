package com.cloud.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.cloud.common.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SentinelGatewayConfig {

    private final ObjectMapper objectMapper;

    @Value("${app.sentinel.gateway.enabled:true}")
    private boolean sentinelGatewayEnabled;

    @Value("${app.sentinel.gateway.default-qps:80}")
    private double defaultQps;

    @Value("${app.sentinel.gateway.interval-sec:1}")
    private int intervalSec;

    @Value("${app.sentinel.gateway.route-ids:user-service-api-v2,product-service-api-v2,order-service-api-v2,payment-service-api-v2,stock-service-api-v2,search-service-api-v2}")
    private String routeIds;

    @PostConstruct
    public void initGatewayRules() {
        if (!sentinelGatewayEnabled) {
            log.info("Sentinel gateway rules disabled.");
            return;
        }
        Set<GatewayFlowRule> rules = parseRouteIds(routeIds).stream()
                .map(routeId -> new GatewayFlowRule(routeId)
                        .setCount(defaultQps)
                        .setIntervalSec(intervalSec))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        GatewayRuleManager.loadRules(rules);
        GatewayCallbackManager.setBlockHandler((exchange, t) -> buildBlockedResponse(t));
        log.info("Sentinel gateway rules loaded: routeCount={}, defaultQps={}, intervalSec={}",
                rules.size(), defaultQps, intervalSec);
    }

    private Set<String> parseRouteIds(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Mono<ServerResponse> buildBlockedResponse(Throwable throwable) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Result.error("Sentinel blocked by gateway rule"));
        } catch (JsonProcessingException e) {
            log.error("Serialize sentinel block response failed", e);
            payload = "{\"code\":429,\"message\":\"sentinel blocked\",\"data\":null}";
        }
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload);
    }
}

