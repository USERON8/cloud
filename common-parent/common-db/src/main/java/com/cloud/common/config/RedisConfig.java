package com.cloud.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

@Configuration
@EnableCaching
@ConditionalOnClass({RedisConnectionFactory.class, RedisTemplate.class})
public class RedisConfig {

  private static final String TYPE_HINT_PROPERTY = "@class";
  private static final String NULL_VALUE_TYPE = "org.springframework.cache.support.NullValue";
  private static final String SPRING_DATA_DOMAIN_TYPE_PREFIX = "org.springframework.data.domain.";

  @Value("${cache.ttl.user:600}") // user-service: 10 minutes
  private long userTtl;

  @Value("${cache.ttl.product:2700}")
  private long productTtl;

  @Value("${cache.ttl.stock:300}")
  private long stockTtl;

  @Value("${cache.ttl.order:3600}") // order-service: 1 hour
  private long orderTtl;

  @Value("${cache.ttl.payment:600}")
  private long paymentTtl;

  @Value("${cache.ttl.search:600}") // search-service: 2-10 minutes, default 10 minutes
  private long searchTtl;

  @Value("${cache.ttl.auth:3600}")
  private long authTtl;

  @Bean
  @ConditionalOnMissingBean
  public GenericJackson2JsonRedisSerializer jsonRedisSerializer() {
    ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    // Keep shared DTO and collection cache round-trips working while rejecting arbitrary type
    // materialization from Redis payloads.
    GenericJackson2JsonRedisSerializer.registerNullValueSerializer(
        objectMapper, TYPE_HINT_PROPERTY);
    objectMapper.activateDefaultTypingAsProperty(
        redisTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL, TYPE_HINT_PROPERTY);
    objectMapper.findAndRegisterModules();
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    return new GenericJackson2JsonRedisSerializer(objectMapper);
  }

  @Bean
  @Primary
  @ConditionalOnMissingBean
  public RedisTemplate<String, Object> redisTemplate(
      RedisConnectionFactory connectionFactory, GenericJackson2JsonRedisSerializer jsonSerializer) {

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
      RedisConnectionFactory connectionFactory, GenericJackson2JsonRedisSerializer jsonSerializer) {
    RedisCacheConfiguration defaultConfig =
        createCacheConfig(jsonSerializer, Duration.ofMinutes(30));
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

    cacheConfigurations.put("user", createCacheConfig(jsonSerializer, Duration.ofSeconds(userTtl)));
    cacheConfigurations.put(
        "product", createCacheConfig(jsonSerializer, Duration.ofSeconds(productTtl)));
    cacheConfigurations.put(
        "stock", createCacheConfig(jsonSerializer, Duration.ofSeconds(stockTtl)));
    cacheConfigurations.put(
        "order", createCacheConfig(jsonSerializer, Duration.ofSeconds(orderTtl)));
    cacheConfigurations.put(
        "payment", createCacheConfig(jsonSerializer, Duration.ofSeconds(paymentTtl)));
    cacheConfigurations.put(
        "search", createCacheConfig(jsonSerializer, Duration.ofSeconds(searchTtl)));
    cacheConfigurations.put("auth", createCacheConfig(jsonSerializer, Duration.ofSeconds(authTtl)));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigurations)
        .transactionAware()
        .build();
  }

  private RedisCacheConfiguration createCacheConfig(
      GenericJackson2JsonRedisSerializer jsonSerializer, Duration ttl) {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(ttl)
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
        .disableCachingNullValues();
  }

  private BasicPolymorphicTypeValidator redisTypeValidator() {
    return BasicPolymorphicTypeValidator.builder()
        .allowIfSubType("com.cloud.")
        .allowIfSubType(SPRING_DATA_DOMAIN_TYPE_PREFIX)
        .allowIfSubType(NULL_VALUE_TYPE)
        .allowIfSubType("java.math.")
        .allowIfSubType("java.time.")
        .allowIfSubType(Collection.class)
        .allowIfSubType(Map.class)
        .allowIfSubType(Date.class)
        .allowIfSubTypeIsArray()
        .build();
  }
}
