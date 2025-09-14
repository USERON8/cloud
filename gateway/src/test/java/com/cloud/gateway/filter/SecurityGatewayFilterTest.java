package com.cloud.gateway.filter;

import com.cloud.gateway.monitoring.GatewayPerformanceMonitor;
import com.cloud.gateway.security.GatewayRateLimitManager;
import com.cloud.gateway.security.GatewaySecurityAccessManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 安全网关过滤器测试类
 *
 * @author what's up
 */
class SecurityGatewayFilterTest {

    @Mock
    private GatewaySecurityAccessManager securityAccessManager;

    @Mock
    private GatewayRateLimitManager rateLimitManager;

    @Mock
    private GatewayPerformanceMonitor performanceMonitor;

    @Mock
    private GatewayFilterChain filterChain;

    private SecurityGatewayFilter securityGatewayFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        securityGatewayFilter = new SecurityGatewayFilter(
                securityAccessManager, rateLimitManager
        );
        // 手动设置性能监控器（由于是可选的）
        // 这里不需要设置，因为测试中禁用了性能监控
    }

    @Test
    void testIpAccessAllowed() throws UnknownHostException {
        // 准备测试数据
        SecurityGatewayFilter.Config config = new SecurityGatewayFilter.Config();
        config.setEnableIpCheck(true);
        config.setEnableRateLimit(false);
        config.setEnableTokenCheck(false);
        config.setEnablePerformanceMonitoring(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "/api/test")
                .remoteAddress(new InetSocketAddress(InetAddress.getByName("192.168.1.1"), 8080))
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewaySecurityAccessManager.AccessCheckResult allowedResult =
                new GatewaySecurityAccessManager.AccessCheckResult(true, "IP访问检查通过");

        when(securityAccessManager.checkIpAccess(anyString(), any())).thenReturn(allowedResult);
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        // 执行测试
        GatewayFilter filter = securityGatewayFilter.apply(config);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        // 验证
        verify(securityAccessManager).checkIpAccess(eq("192.168.1.1"), any());
        verify(filterChain).filter(exchange);
    }

    @Test
    void testIpAccessBlocked() throws UnknownHostException {
        // 准备测试数据
        SecurityGatewayFilter.Config config = new SecurityGatewayFilter.Config();
        config.setEnableIpCheck(true);

        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "/api/test")
                .remoteAddress(new InetSocketAddress(InetAddress.getByName("192.168.1.1"), 8080))
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewaySecurityAccessManager.AccessCheckResult blockedResult =
                new GatewaySecurityAccessManager.AccessCheckResult(false, "IP在黑名单中");

        when(securityAccessManager.checkIpAccess(anyString(), any())).thenReturn(blockedResult);

        // 执行测试
        GatewayFilter filter = securityGatewayFilter.apply(config);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        // 验证响应状态
        ServerHttpResponse response = exchange.getResponse();
        assert response.getStatusCode() == HttpStatus.FORBIDDEN;

        // 验证不会继续执行过滤器链
        verify(filterChain, never()).filter(exchange);
    }

    @Test
    void testRateLimitAllowed() throws UnknownHostException {
        // 准备测试数据
        SecurityGatewayFilter.Config config = new SecurityGatewayFilter.Config();
        config.setEnableIpCheck(false);
        config.setEnableRateLimit(true);
        config.setEnableTokenCheck(false);
        config.setEnablePerformanceMonitoring(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "/api/test")
                .remoteAddress(new InetSocketAddress(InetAddress.getByName("192.168.1.1"), 8080))
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayRateLimitManager.RateLimitResult allowedResult =
                new GatewayRateLimitManager.RateLimitResult(true, 10, 
                        System.currentTimeMillis() + 60000, "限流检查通过");

        when(rateLimitManager.checkLimit(anyString(), anyString()))
                .thenReturn(reactor.core.publisher.Mono.just(allowedResult));
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        // 执行测试
        GatewayFilter filter = securityGatewayFilter.apply(config);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        // 验证限流响应头被添加
        HttpHeaders headers = exchange.getResponse().getHeaders();
        assert headers.containsKey("X-RateLimit-Remaining");
        assert headers.containsKey("X-RateLimit-Reset");

        verify(filterChain).filter(exchange);
    }

    @Test
    void testRateLimitExceeded() throws UnknownHostException {
        // 准备测试数据
        SecurityGatewayFilter.Config config = new SecurityGatewayFilter.Config();
        config.setEnableIpCheck(false);
        config.setEnableRateLimit(true);

        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "/api/test")
                .remoteAddress(new InetSocketAddress(InetAddress.getByName("192.168.1.1"), 8080))
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayRateLimitManager.RateLimitResult exceededResult =
                new GatewayRateLimitManager.RateLimitResult(false, 0, 
                        System.currentTimeMillis() + 60000, "超出限流阈值");

        when(rateLimitManager.checkLimit(anyString(), anyString()))
                .thenReturn(reactor.core.publisher.Mono.just(exceededResult));

        // 执行测试
        GatewayFilter filter = securityGatewayFilter.apply(config);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        // 验证响应状态
        ServerHttpResponse response = exchange.getResponse();
        assert response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;

        // 验证限流响应头被添加
        HttpHeaders headers = response.getHeaders();
        assert headers.containsKey("X-RateLimit-Remaining");
        assert headers.containsKey("X-RateLimit-Reset");

        verify(filterChain, never()).filter(exchange);
    }

    @Test
    void testGetClientIpFromXForwardedFor() throws UnknownHostException {
        // 测试从 X-Forwarded-For获取客户端IP
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "/api/test")
                .header("X-Forwarded-For", "203.208.60.1, 192.168.1.100")
                .remoteAddress(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8080))
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        SecurityGatewayFilter.Config config = new SecurityGatewayFilter.Config();
        config.setEnableIpCheck(true);
        config.setEnableRateLimit(false);
        config.setEnableTokenCheck(false);
        config.setEnablePerformanceMonitoring(false);

        GatewaySecurityAccessManager.AccessCheckResult allowedResult =
                new GatewaySecurityAccessManager.AccessCheckResult(true, "IP访问检查通过");

        when(securityAccessManager.checkIpAccess(eq("203.208.60.1"), any())).thenReturn(allowedResult);
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        // 执行测试
        GatewayFilter filter = securityGatewayFilter.apply(config);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        // 验证使用了X-Forwarded-For的第一个IP
        verify(securityAccessManager).checkIpAccess(eq("203.208.60.1"), any());
    }

    @Test
    void testDetermineRateLimitKey() throws UnknownHostException {
        SecurityGatewayFilter.Config config = new SecurityGatewayFilter.Config();
        config.setEnableIpCheck(false);
        config.setEnableRateLimit(true);
        config.setEnableTokenCheck(false);
        config.setEnablePerformanceMonitoring(false);

        // 测试登录接口
        MockServerHttpRequest loginRequest = MockServerHttpRequest
                .method(HttpMethod.POST, "/auth/login")
                .remoteAddress(new InetSocketAddress(InetAddress.getByName("192.168.1.1"), 8080))
                .build();

        ServerWebExchange loginExchange = MockServerWebExchange.from(loginRequest);

        GatewayRateLimitManager.RateLimitResult allowedResult =
                new GatewayRateLimitManager.RateLimitResult(true, 10, 
                        System.currentTimeMillis() + 60000, "限流检查通过");

        when(rateLimitManager.checkLimit(eq("auth:login"), anyString()))
                .thenReturn(reactor.core.publisher.Mono.just(allowedResult));
        when(filterChain.filter(loginExchange)).thenReturn(Mono.empty());

        GatewayFilter filter = securityGatewayFilter.apply(config);

        StepVerifier.create(filter.filter(loginExchange, filterChain))
                .verifyComplete();

        verify(rateLimitManager).checkLimit(eq("auth:login"), anyString());
    }
}
