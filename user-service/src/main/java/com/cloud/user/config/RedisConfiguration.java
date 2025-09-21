package com.cloud.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 用户服务Redis配置
 * 支持多级缓存架构，优化用户相关数据的存储
 * 使用Hash类型存储用户高频访问字段
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class RedisConfiguration {

    /**
     * 用户服务Redis模板配置
     */
    @Bean
    public RedisTemplate<String, Object> userRedisTemplate(RedisConnectionFactory redisConnectionFactory,
                                                           ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 设置key和value的序列化规则
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 字符串Redis模板，用于简单缓存操作
     */
    @Bean
    public StringRedisTemplate userStringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    /**
     * Hash操作接口，用于存储用户详细信息
     */
    @Bean
    public HashOperations<String, String, Object> userHashOperations(
            RedisTemplate<String, Object> userRedisTemplate) {
        return userRedisTemplate.opsForHash();
    }
}
