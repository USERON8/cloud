package com.cloud.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthUserAuthorityCacheService {

    private static final String AUTH_USER_PREFIX = "auth:user:";
    private static final Duration AUTHORITY_TTL = Duration.ofMinutes(30);

    @Qualifier("oauth2MainRedisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;

    public List<SimpleGrantedAuthority> loadAuthorities(Long userId) {
        if (userId == null) {
            return List.of();
        }
        Object cached = redisTemplate.opsForValue().get(AUTH_USER_PREFIX + userId);
        if (!(cached instanceof Collection<?> cachedCollection)) {
            return List.of();
        }
        Set<String> authorities = cachedCollection.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (authorities.isEmpty()) {
            return List.of();
        }
        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    public void cacheAuthorities(Long userId, Collection<? extends GrantedAuthority> authorities) {
        if (userId == null || authorities == null || authorities.isEmpty()) {
            return;
        }
        Set<String> values = authorities.stream()
                .filter(Objects::nonNull)
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (values.isEmpty()) {
            return;
        }
        redisTemplate.opsForValue().set(AUTH_USER_PREFIX + userId, values, AUTHORITY_TTL);
    }

    public void evict(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.delete(AUTH_USER_PREFIX + userId);
    }
}
