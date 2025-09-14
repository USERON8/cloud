package com.cloud.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 网关安全访问管理器
 * 处理IP访问控制、Token撤销检查等安全功能
 * 
 * @author what's up
 */
@Slf4j
@Component
public class GatewaySecurityAccessManager {

    private final ReactiveStringRedisTemplate redisTemplate;
    
    // IP白名单和黑名单
    @Value("#{'${gateway.security.ip.whitelist:}'.split(',')}")
    private List<String> ipWhitelist;
    
    @Value("#{'${gateway.security.ip.blacklist:}'.split(',')}")
    private List<String> ipBlacklist;
    
    // 可疑User-Agent黑名单
    private static final Set<String> SUSPICIOUS_USER_AGENTS = Set.of(
            "sqlmap", "nikto", "nmap", "masscan", "zap",
            "burp", "dirbuster", "gobuster", "wfuzz"
    );
    
    // IP访问频率限制 (内存缓存)
    private final ConcurrentHashMap<String, Integer> ipAccessCount = new ConcurrentHashMap<>();
    
    // Redis键前缀
    private static final String TOKEN_REVOKE_PREFIX = "gateway:token:revoked:";
    private static final String USER_TOKENS_REVOKE_PREFIX = "gateway:user:tokens:revoked:";
    
    public GatewaySecurityAccessManager(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 检查IP访问权限
     */
    public AccessCheckResult checkIpAccess(String clientIp, String userAgent) {
        log.debug("检查IP访问权限: IP={}, UserAgent={}", clientIp, userAgent);
        
        // 1. 检查黑名单
        if (isIpInList(clientIp, ipBlacklist)) {
            return new AccessCheckResult(false, "IP在黑名单中");
        }
        
        // 2. 如果配置了白名单，检查白名单
        if (!ipWhitelist.isEmpty() && !ipWhitelist.contains("") && !isIpInList(clientIp, ipWhitelist)) {
            return new AccessCheckResult(false, "IP不在白名单中");
        }
        
        // 3. 检查可疑User-Agent
        if (userAgent != null && isSuspiciousUserAgent(userAgent)) {
            log.warn("检测到可疑User-Agent: IP={}, UserAgent={}", clientIp, userAgent);
            return new AccessCheckResult(false, "可疑User-Agent");
        }
        
        // 4. 检查IP访问频率（简单的内存限制）
        int currentCount = ipAccessCount.getOrDefault(clientIp, 0);
        if (currentCount > 1000) { // 简单的频率限制
            return new AccessCheckResult(false, "IP访问频率过高");
        }
        
        // 更新访问计数
        ipAccessCount.put(clientIp, currentCount + 1);
        
        return new AccessCheckResult(true, "IP访问检查通过");
    }
    
    /**
     * 检查Token是否被撤销
     */
    public Mono<Boolean> isTokenRevoked(String tokenValue) {
        String key = TOKEN_REVOKE_PREFIX + tokenValue;
        return redisTemplate.hasKey(key)
                .defaultIfEmpty(false)
                .doOnNext(revoked -> {
                    if (revoked) {
                        log.debug("Token已被撤销: {}", tokenValue.substring(0, Math.min(20, tokenValue.length())));
                    }
                })
                .onErrorReturn(false);
    }
    
    /**
     * 检查用户的所有Token是否被撤销
     */
    public Mono<Boolean> isUserTokensRevoked(String userId) {
        String key = USER_TOKENS_REVOKE_PREFIX + userId;
        return redisTemplate.hasKey(key)
                .defaultIfEmpty(false)
                .doOnNext(revoked -> {
                    if (revoked) {
                        log.debug("用户所有Token已被撤销: userId={}", userId);
                    }
                })
                .onErrorReturn(false);
    }
    
    /**
     * 撤销指定Token
     */
    public Mono<Void> revokeToken(String tokenValue, Duration expiration) {
        String key = TOKEN_REVOKE_PREFIX + tokenValue;
        return redisTemplate.opsForValue().set(key, "revoked", expiration).then();
    }
    
    /**
     * 撤销用户所有Token
     */
    public Mono<Void> revokeUserTokens(String userId, Duration expiration) {
        String key = USER_TOKENS_REVOKE_PREFIX + userId;
        return redisTemplate.opsForValue().set(key, "revoked", expiration).then();
    }
    
    /**
     * 检查IP是否在指定列表中
     */
    private boolean isIpInList(String clientIp, List<String> ipList) {
        if (ipList == null || ipList.isEmpty()) {
            return false;
        }
        
        return ipList.stream()
                .filter(ip -> ip != null && !ip.trim().isEmpty())
                .anyMatch(ip -> {
                    try {
                        if (ip.contains("/")) {
                            // CIDR格式支持（简单实现）
                            return isIpInCidr(clientIp, ip);
                        } else if (ip.contains("*")) {
                            // 通配符支持
                            String regex = ip.replace(".", "\\.")
                                           .replace("*", "\\d+");
                            return Pattern.matches(regex, clientIp);
                        } else {
                            // 精确匹配
                            return ip.equals(clientIp);
                        }
                    } catch (Exception e) {
                        log.warn("IP匹配异常: IP={}, Pattern={}", clientIp, ip, e);
                        return false;
                    }
                });
    }
    
    /**
     * 检查IP是否在CIDR网段中
     */
    private boolean isIpInCidr(String ip, String cidr) {
        // 简化实现，实际生产环境建议使用专业的IP工具库
        try {
            String[] parts = cidr.split("/");
            String network = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);
            
            // 这里简化处理，实际应该进行位运算
            if (prefixLength >= 24) {
                String ipPrefix = ip.substring(0, ip.lastIndexOf("."));
                String networkPrefix = network.substring(0, network.lastIndexOf("."));
                return ipPrefix.equals(networkPrefix);
            }
        } catch (Exception e) {
            log.warn("CIDR匹配异常: IP={}, CIDR={}", ip, cidr, e);
        }
        return false;
    }
    
    /**
     * 检查是否是可疑User-Agent
     */
    private boolean isSuspiciousUserAgent(String userAgent) {
        String lowerUserAgent = userAgent.toLowerCase();
        return SUSPICIOUS_USER_AGENTS.stream()
                .anyMatch(lowerUserAgent::contains);
    }
    
    /**
     * 清理IP访问计数缓存（定期调用）
     */
    public void cleanupIpAccessCount() {
        if (ipAccessCount.size() > 10000) {
            log.info("清理IP访问计数缓存，当前大小: {}", ipAccessCount.size());
            ipAccessCount.clear();
        }
    }
    
    /**
     * 访问检查结果
     */
    public static class AccessCheckResult {
        private final boolean allowed;
        private final String reason;
        
        public AccessCheckResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public String getReason() {
            return reason;
        }
        
        @Override
        public String toString() {
            return "AccessCheckResult{allowed=" + allowed + ", reason='" + reason + "'}";
        }
    }
}
