package com.cloud.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 安全访问管理器
 * 提供IP白名单、黑名单、Token撤销等安全控制功能
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityAccessManager {

    // Redis键前缀
    private static final String IP_WHITELIST_KEY = "security:ip:whitelist";
    private static final String IP_BLACKLIST_KEY = "security:ip:blacklist";
    private static final String TOKEN_BLACKLIST_PREFIX = "security:token:blacklist:";
    private static final String SUSPICIOUS_IP_PREFIX = "security:suspicious:";
    private static final String ACCESS_LOG_PREFIX = "security:access_log:";
    private final RedisTemplate<String, Object> redisTemplate;
    // IP白名单缓存
    private final Set<String> ipWhitelist = ConcurrentHashMap.newKeySet();
    // IP黑名单缓存
    private final Set<String> ipBlacklist = ConcurrentHashMap.newKeySet();
    // IP地址模式缓存
    private final Map<String, Pattern> ipPatterns = new ConcurrentHashMap<>();
    // 访问统计
    private final Map<String, AccessStats> accessStats = new ConcurrentHashMap<>();

    /**
     * 检查IP访问权限
     *
     * @param clientIp  客户端IP
     * @param userAgent 用户代理
     * @return 访问检查结果
     */
    public AccessCheckResult checkIpAccess(String clientIp, String userAgent) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            return AccessCheckResult.block("无效的IP地址", AccessCheckResult.AccessAction.BLOCK_IP);
        }

        // 记录访问统计
        AccessStats stats = accessStats.computeIfAbsent(clientIp, k -> new AccessStats());
        stats.recordAccess(userAgent);

        try {
            // 检查IP黑名单
            if (isInBlacklist(clientIp)) {
                stats.recordBlocked();
                logSecurityEvent(clientIp, "IP_BLOCKED", "IP在黑名单中");
                return AccessCheckResult.block("IP地址被禁止访问", AccessCheckResult.AccessAction.BLOCK_IP);
            }

            // 检查IP白名单（如果启用了白名单模式）
            if (!ipWhitelist.isEmpty() && !isInWhitelist(clientIp)) {
                stats.recordBlocked();
                logSecurityEvent(clientIp, "IP_NOT_WHITELISTED", "IP不在白名单中");
                return AccessCheckResult.block("IP地址不在白名单中", AccessCheckResult.AccessAction.BLOCK_IP);
            }

            // 检查可疑活动
            if (isSuspiciousActivity(clientIp, stats)) {
                stats.recordSuspicious();
                logSecurityEvent(clientIp, "SUSPICIOUS_ACTIVITY", "检测到可疑活动");
                return AccessCheckResult.block("检测到可疑活动", AccessCheckResult.AccessAction.SUSPICIOUS);
            }

            // 记录访问日志
            logAccess(clientIp, userAgent);

            return AccessCheckResult.allow();

        } catch (Exception e) {
            log.error("IP访问检查异常, IP: {}", clientIp, e);
            // 异常时允许访问（fail-open策略）
            return AccessCheckResult.allow();
        }
    }

    /**
     * 检查Token是否被撤销
     *
     * @param tokenValue Token值
     * @return 是否被撤销
     */
    public boolean isTokenRevoked(String tokenValue) {
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            return false;
        }

        try {
            String tokenHash = generateTokenHash(tokenValue);
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + tokenHash;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            log.error("Token撤销检查异常, token: {}", tokenValue.substring(0, 10) + "...", e);
            return false;
        }
    }

    /**
     * 撤销Token
     *
     * @param tokenValue Token值
     * @param expireAt   Token过期时间
     * @param reason     撤销原因
     */
    public void revokeToken(String tokenValue, Instant expireAt, String reason) {
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            return;
        }

        try {
            String tokenHash = generateTokenHash(tokenValue);
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + tokenHash;

            Map<String, Object> revokeInfo = new HashMap<>();
            revokeInfo.put("reason", reason);
            revokeInfo.put("revokedAt", Instant.now().toString());
            revokeInfo.put("expireAt", expireAt.toString());

            // 计算过期时间（Token原本的过期时间）
            long ttl = Duration.between(Instant.now(), expireAt).getSeconds();
            if (ttl > 0) {
                redisTemplate.opsForValue().set(blacklistKey, revokeInfo, Duration.ofSeconds(ttl));
            }

            log.info("Token已撤销: hash={}, reason={}", tokenHash, reason);

        } catch (Exception e) {
            log.error("撤销Token异常, token: {}", tokenValue.substring(0, 10) + "...", e);
        }
    }

    /**
     * 批量撤销用户的所有Token
     *
     * @param userId 用户ID
     * @param reason 撤销原因
     */
    public void revokeAllUserTokens(String userId, String reason) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        try {
            // 在Redis中设置用户Token撤销标记
            String userRevokeKey = "security:user_token_revoked:" + userId;
            Map<String, Object> revokeInfo = new HashMap<>();
            revokeInfo.put("reason", reason);
            revokeInfo.put("revokedAt", Instant.now().toString());

            // 设置过期时间为24小时（足够覆盖大部分Token的生命周期）
            redisTemplate.opsForValue().set(userRevokeKey, revokeInfo, Duration.ofHours(24));

            log.info("用户所有Token已撤销: userId={}, reason={}", userId, reason);

        } catch (Exception e) {
            log.error("批量撤销用户Token异常, userId: {}", userId, e);
        }
    }

    /**
     * 检查用户的所有Token是否被撤销
     *
     * @param userId 用户ID
     * @return 是否被撤销
     */
    public boolean isUserTokensRevoked(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        try {
            String userRevokeKey = "security:user_token_revoked:" + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(userRevokeKey));
        } catch (Exception e) {
            log.error("用户Token撤销检查异常, userId: {}", userId, e);
            return false;
        }
    }

    /**
     * 添加IP到白名单
     *
     * @param ip IP地址或IP段（支持CIDR表示法）
     */
    public void addToWhitelist(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return;
        }

        ipWhitelist.add(ip.trim());
        redisTemplate.opsForSet().add(IP_WHITELIST_KEY, ip.trim());

        log.info("IP已添加到白名单: {}", ip);
    }

    /**
     * 从白名单移除IP
     *
     * @param ip IP地址
     */
    public void removeFromWhitelist(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return;
        }

        ipWhitelist.remove(ip.trim());
        redisTemplate.opsForSet().remove(IP_WHITELIST_KEY, ip.trim());

        log.info("IP已从白名单移除: {}", ip);
    }

    /**
     * 添加IP到黑名单
     *
     * @param ip       IP地址
     * @param reason   封禁原因
     * @param duration 封禁时长
     */
    public void addToBlacklist(String ip, String reason, Duration duration) {
        if (ip == null || ip.trim().isEmpty()) {
            return;
        }

        ipBlacklist.add(ip.trim());

        Map<String, Object> blacklistInfo = new HashMap<>();
        blacklistInfo.put("reason", reason);
        blacklistInfo.put("blockedAt", Instant.now().toString());

        if (duration != null) {
            redisTemplate.opsForValue().set(IP_BLACKLIST_KEY + ":" + ip.trim(), blacklistInfo, duration);
        } else {
            redisTemplate.opsForSet().add(IP_BLACKLIST_KEY, ip.trim());
        }

        logSecurityEvent(ip, "IP_BLACKLISTED", reason);
        log.info("IP已添加到黑名单: {}, reason: {}, duration: {}", ip, reason, duration);
    }

    /**
     * 从黑名单移除IP
     *
     * @param ip IP地址
     */
    public void removeFromBlacklist(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return;
        }

        ipBlacklist.remove(ip.trim());
        redisTemplate.opsForSet().remove(IP_BLACKLIST_KEY, ip.trim());
        redisTemplate.delete(IP_BLACKLIST_KEY + ":" + ip.trim());

        log.info("IP已从黑名单移除: {}", ip);
    }

    /**
     * 检查IP是否在白名单中
     */
    private boolean isInWhitelist(String ip) {
        // 精确匹配
        if (ipWhitelist.contains(ip)) {
            return true;
        }

        // 模式匹配（支持CIDR等）
        return ipWhitelist.stream().anyMatch(pattern -> matchIpPattern(ip, pattern));
    }

    /**
     * 检查IP是否在黑名单中
     */
    private boolean isInBlacklist(String ip) {
        // 检查内存缓存
        if (ipBlacklist.contains(ip)) {
            return true;
        }

        // 检查Redis中的临时黑名单
        String tempBlacklistKey = IP_BLACKLIST_KEY + ":" + ip;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(tempBlacklistKey))) {
            return true;
        }

        // 检查永久黑名单
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(IP_BLACKLIST_KEY, ip));
    }

    /**
     * IP模式匹配
     */
    private boolean matchIpPattern(String ip, String pattern) {
        try {
            // 支持CIDR表示法
            if (pattern.contains("/")) {
                return matchCIDR(ip, pattern);
            }

            // 支持通配符
            if (pattern.contains("*")) {
                Pattern regex = ipPatterns.computeIfAbsent(pattern,
                        p -> Pattern.compile(p.replace(".", "\\.").replace("*", ".*")));
                return regex.matcher(ip).matches();
            }

            return ip.equals(pattern);

        } catch (Exception e) {
            log.warn("IP模式匹配异常, ip: {}, pattern: {}", ip, pattern, e);
            return false;
        }
    }

    /**
     * CIDR匹配
     */
    private boolean matchCIDR(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }

            String networkIp = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            // 简化的CIDR匹配实现
            // 实际项目中建议使用专门的IP库如Apache Commons IP
            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(networkIp);
            long mask = 0xFFFFFFFFL << (32 - prefixLength);

            return (ipLong & mask) == (networkLong & mask);

        } catch (Exception e) {
            log.warn("CIDR匹配异常, ip: {}, cidr: {}", ip, cidr, e);
            return false;
        }
    }

    /**
     * IP地址转换为长整型
     */
    private long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IP address: " + ip);
        }

        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = result << 8 | Integer.parseInt(parts[i]);
        }
        return result;
    }

    /**
     * 检查是否有可疑活动
     */
    private boolean isSuspiciousActivity(String ip, AccessStats stats) {
        // 检查是否已标记为可疑
        String suspiciousKey = SUSPICIOUS_IP_PREFIX + ip;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(suspiciousKey))) {
            return true;
        }

        // 检查访问频率是否异常
        if (stats.getTotalRequests() > 100 &&
                stats.getFirstAccessTime() != null &&
                Duration.between(stats.getFirstAccessTime(), Instant.now()).toMinutes() < 5) {
            // 5分钟内超过100次请求
            markAsSuspicious(ip, "高频访问", Duration.ofHours(1));
            return true;
        }

        // 检查User-Agent是否异常
        Set<String> userAgents = stats.getUserAgents();
        if (userAgents.size() > 10) {
            // 同一IP使用超过10个不同的User-Agent
            markAsSuspicious(ip, "User-Agent异常", Duration.ofHours(2));
            return true;
        }

        return false;
    }

    /**
     * 标记为可疑IP
     */
    private void markAsSuspicious(String ip, String reason, Duration duration) {
        String suspiciousKey = SUSPICIOUS_IP_PREFIX + ip;
        Map<String, Object> suspiciousInfo = new HashMap<>();
        suspiciousInfo.put("reason", reason);
        suspiciousInfo.put("markedAt", Instant.now().toString());

        redisTemplate.opsForValue().set(suspiciousKey, suspiciousInfo, duration);
        log.warn("IP已标记为可疑: {}, reason: {}", ip, reason);
    }

    /**
     * 生成Token哈希
     */
    private String generateTokenHash(String token) {
        // 使用简单的哈希算法，实际项目中建议使用SHA-256
        return String.valueOf(token.hashCode());
    }

    /**
     * 记录安全事件
     */
    private void logSecurityEvent(String ip, String event, String details) {
        try {
            String logKey = "security:events:" + Instant.now().toString().substring(0, 10); // 按日期分组
            Map<String, Object> eventLog = new HashMap<>();
            eventLog.put("ip", ip);
            eventLog.put("event", event);
            eventLog.put("details", details);
            eventLog.put("timestamp", Instant.now().toString());

            redisTemplate.opsForList().leftPush(logKey, eventLog);
            redisTemplate.expire(logKey, Duration.ofDays(7)); // 保留7天

        } catch (Exception e) {
            log.error("记录安全事件失败", e);
        }
    }

    /**
     * 记录访问日志
     */
    private void logAccess(String ip, String userAgent) {
        try {
            String logKey = ACCESS_LOG_PREFIX + Instant.now().toString().substring(0, 10);
            Map<String, Object> accessLog = new HashMap<>();
            accessLog.put("ip", ip);
            accessLog.put("userAgent", userAgent);
            accessLog.put("timestamp", Instant.now().toString());

            redisTemplate.opsForList().leftPush(logKey, accessLog);
            redisTemplate.expire(logKey, Duration.ofDays(3)); // 访问日志保留3天

        } catch (Exception e) {
            log.error("记录访问日志失败", e);
        }
    }

    /**
     * 获取访问统计信息
     *
     * @return 访问统计信息
     */
    public Map<String, AccessStats> getAccessStats() {
        return new HashMap<>(accessStats);
    }

    /**
     * 获取白名单列表
     *
     * @return 白名单列表
     */
    public Set<String> getWhitelist() {
        return new HashSet<>(ipWhitelist);
    }

    /**
     * 获取黑名单列表
     *
     * @return 黑名单列表
     */
    public Set<String> getBlacklist() {
        return new HashSet<>(ipBlacklist);
    }

    /**
     * 初始化安全规则
     */
    public void initSecurityRules() {
        try {
            // 从Redis加载白名单
            Set<Object> whitelistFromRedis = redisTemplate.opsForSet().members(IP_WHITELIST_KEY);
            if (whitelistFromRedis != null) {
                whitelistFromRedis.stream()
                        .map(Object::toString)
                        .forEach(ipWhitelist::add);
            }

            // 从Redis加载黑名单
            Set<Object> blacklistFromRedis = redisTemplate.opsForSet().members(IP_BLACKLIST_KEY);
            if (blacklistFromRedis != null) {
                blacklistFromRedis.stream()
                        .map(Object::toString)
                        .forEach(ipBlacklist::add);
            }

            log.info("安全规则初始化完成, 白名单: {}, 黑名单: {}", ipWhitelist.size(), ipBlacklist.size());

        } catch (Exception e) {
            log.error("安全规则初始化失败", e);
        }
    }

    /**
     * 访问统计信息
     */
    public static class AccessStats {
        private long totalRequests;
        private long suspiciousRequests;
        private long blockedRequests;
        private Instant firstAccessTime;
        private Instant lastAccessTime;
        private Set<String> userAgents = ConcurrentHashMap.newKeySet();

        public void recordAccess(String userAgent) {
            totalRequests++;
            lastAccessTime = Instant.now();
            if (firstAccessTime == null) {
                firstAccessTime = Instant.now();
            }
            if (userAgent != null) {
                userAgents.add(userAgent);
            }
        }

        public void recordSuspicious() {
            suspiciousRequests++;
        }

        public void recordBlocked() {
            blockedRequests++;
        }

        public double getSuspiciousRate() {
            return totalRequests > 0 ? (double) suspiciousRequests / totalRequests : 0.0;
        }

        // Getters
        public long getTotalRequests() {
            return totalRequests;
        }

        public long getSuspiciousRequests() {
            return suspiciousRequests;
        }

        public long getBlockedRequests() {
            return blockedRequests;
        }

        public Instant getFirstAccessTime() {
            return firstAccessTime;
        }

        public Instant getLastAccessTime() {
            return lastAccessTime;
        }

        public Set<String> getUserAgents() {
            return new HashSet<>(userAgents);
        }
    }

    /**
     * 访问检查结果
     */
    public static class AccessCheckResult {
        private final boolean allowed;
        private final String reason;
        private final AccessAction action;

        public AccessCheckResult(boolean allowed, String reason, AccessAction action) {
            this.allowed = allowed;
            this.reason = reason;
            this.action = action;
        }

        public static AccessCheckResult allow() {
            return new AccessCheckResult(true, "访问允许", AccessAction.ALLOW);
        }

        public static AccessCheckResult block(String reason, AccessAction action) {
            return new AccessCheckResult(false, reason, action);
        }

        // Getters
        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }

        public AccessAction getAction() {
            return action;
        }

        public enum AccessAction {
            ALLOW,           // 允许访问
            BLOCK_IP,        // IP黑名单阻止
            BLOCK_TOKEN,     // Token黑名单阻止
            SUSPICIOUS,      // 可疑活动
            RATE_LIMITED     // 限流阻止
        }
    }
}
