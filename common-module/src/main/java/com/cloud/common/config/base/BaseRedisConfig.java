package com.cloud.common.config.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;












@Slf4j
public abstract class BaseRedisConfig {

    






    protected RedisTemplate<String, Object> createRedisTemplateBean(RedisConnectionFactory redisConnectionFactory) {
        
        return createRedisTemplate(redisConnectionFactory);
    }

    






    protected StringRedisTemplate createStringRedisTemplateBean(RedisConnectionFactory redisConnectionFactory) {
        
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        template.setEnableTransactionSupport(shouldEnableTransactionSupport());
        template.afterPropertiesSet();
        return template;
    }

    






    protected RedisTemplate<String, Object> createRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());

        
        template.setDefaultSerializer(RedisSerializer.json());

        
        template.setEnableTransactionSupport(shouldEnableTransactionSupport());

        
        template.afterPropertiesSet();

        return template;
    }

    





    protected boolean shouldEnableTransactionSupport() {
        return false;
    }

    





    protected String getServicePrefix() {
        return "default";
    }

    







    protected String buildCacheKey(String type, String key) {
        return String.format("%s:%s:%s", getServicePrefix(), type, key);
    }

    






    protected long getCacheExpireTime(String type) {
        
        return 3600L;
    }
}
