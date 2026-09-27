package com.cloud.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

@AutoConfiguration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnClass({RedisTemplate.class, LettuceConnectionFactory.class})
@EnableConfigurationProperties(RedisProperties.class)
public class SecurityRedisAutoConfiguration {

  @Bean(name = "securityRedisConnectionFactory", defaultCandidate = false)
  @ConditionalOnMissingBean(name = "securityRedisConnectionFactory")
  LettuceConnectionFactory securityRedisConnectionFactory(
      RedisProperties redisProperties,
      @Value("${app.security.redis.database:15}") int securityDatabase) {
    RedisStandaloneConfiguration configuration =
        new RedisStandaloneConfiguration(redisProperties.getHost(), redisProperties.getPort());
    configuration.setDatabase(securityDatabase);
    if (StringUtils.hasText(redisProperties.getUsername())) {
      configuration.setUsername(redisProperties.getUsername());
    }
    if (redisProperties.getPassword() != null) {
      configuration.setPassword(RedisPassword.of(redisProperties.getPassword()));
    }
    return new LettuceConnectionFactory(configuration);
  }

  @Bean(name = "securityRedisTemplate", defaultCandidate = false)
  @ConditionalOnMissingBean(name = "securityRedisTemplate")
  RedisTemplate<String, Object> securityRedisTemplate(
      @Qualifier("securityRedisConnectionFactory")
          LettuceConnectionFactory securityRedisConnectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(securityRedisConnectionFactory);
    StringRedisSerializer keySerializer = new StringRedisSerializer();
    GenericJackson2JsonRedisSerializer valueSerializer =
        new GenericJackson2JsonRedisSerializer();
    template.setKeySerializer(keySerializer);
    template.setHashKeySerializer(keySerializer);
    template.setValueSerializer(valueSerializer);
    template.setHashValueSerializer(valueSerializer);
    template.afterPropertiesSet();
    return template;
  }

  @Bean(name = "securityStringRedisTemplate", defaultCandidate = false)
  @ConditionalOnMissingBean(name = "securityStringRedisTemplate")
  StringRedisTemplate securityStringRedisTemplate(
      @Qualifier("securityRedisConnectionFactory")
          LettuceConnectionFactory securityRedisConnectionFactory) {
    return new StringRedisTemplate(securityRedisConnectionFactory);
  }

  @Bean(name = "securityReactiveStringRedisTemplate", defaultCandidate = false)
  @ConditionalOnClass(ReactiveStringRedisTemplate.class)
  @ConditionalOnMissingBean(name = "securityReactiveStringRedisTemplate")
  ReactiveStringRedisTemplate securityReactiveStringRedisTemplate(
      @Qualifier("securityRedisConnectionFactory")
          LettuceConnectionFactory securityRedisConnectionFactory) {
    return new ReactiveStringRedisTemplate(securityRedisConnectionFactory);
  }
}
