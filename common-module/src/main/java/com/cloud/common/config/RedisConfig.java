package com.cloud.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

















@Slf4j
@Configuration
@EnableCaching
@ConditionalOnClass({RedisConnectionFactory.class, RedisTemplate.class})
public class RedisConfig {

    
    @Value("${cache.ttl.user:1800}")
    private long userTtl;

    @Value("${cache.ttl.product:2700}")
    private long productTtl;

    @Value("${cache.ttl.stock:300}")
    private long stockTtl;

    @Value("${cache.ttl.order:900}")
    private long orderTtl;

    @Value("${cache.ttl.payment:600}")
    private long paymentTtl;

    @Value("${cache.ttl.search:1200}")
    private long searchTtl;

    @Value("${cache.ttl.auth:3600}")
    private long authTtl;

    



    @Bean
    @ConditionalOnMissingBean
    public GenericJackson2JsonRedisSerializer jsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        
        objectMapper.findAndRegisterModules();

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    



    @Bean
    @Primary
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer jsonSerializer) {

        

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setDefaultSerializer(jsonSerializer);

        
        template.setEnableTransactionSupport(false);

        template.afterPropertiesSet();

        
        return template;
    }

    



    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        

        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();

        
        return template;
    }

    




    @Bean("redisCacheManager")
    @ConditionalOnMissingBean(CacheManager.class) 
    public CacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer jsonSerializer) {
        RedisCacheConfiguration defaultConfig = createCacheConfig(jsonSerializer, Duration.ofMinutes(30));
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("user", createCacheConfig(jsonSerializer, Duration.ofSeconds(userTtl)));
        cacheConfigurations.put("product", createCacheConfig(jsonSerializer, Duration.ofSeconds(productTtl)));
        cacheConfigurations.put("stock", createCacheConfig(jsonSerializer, Duration.ofSeconds(stockTtl)));
        cacheConfigurations.put("order", createCacheConfig(jsonSerializer, Duration.ofSeconds(orderTtl)));
        cacheConfigurations.put("payment", createCacheConfig(jsonSerializer, Duration.ofSeconds(paymentTtl)));
        cacheConfigurations.put("search", createCacheConfig(jsonSerializer, Duration.ofSeconds(searchTtl)));
        cacheConfigurations.put("auth", createCacheConfig(jsonSerializer, Duration.ofSeconds(authTtl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    


    private RedisCacheConfiguration createCacheConfig(GenericJackson2JsonRedisSerializer jsonSerializer, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();
    }
}
