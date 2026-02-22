package com.cloud.auth.config;

import com.cloud.auth.service.RedisOAuth2AuthorizationConsentService;
import com.cloud.auth.service.SimpleRedisHashOAuth2AuthorizationService;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;













@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisOAuth2Config {

    private final RedisConnectionFactory redisConnectionFactory;
    private final RegisteredClientRepository registeredClientRepository;
    private final AuthorizationServerSettings authorizationServerSettings;

    



    @Bean
    public OAuth2AuthorizationService authorizationService() {
        return new SimpleRedisHashOAuth2AuthorizationService(
                oauth2MainRedisTemplate(),
                registeredClientRepository,
                authorizationServerSettings
        );
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService() {
        return new RedisOAuth2AuthorizationConsentService(oauth2MainRedisTemplate());
    }

    



    @Bean("oauth2MainRedisTemplate")
    public RedisTemplate<String, Object> oauth2MainRedisTemplate() {
        

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        
        GenericJackson2JsonRedisSerializer jsonRedisSerializer =
                new GenericJackson2JsonRedisSerializer();

        
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);

        
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();

        
        return template;
    }

    



    @Bean("oauth2RedisTemplate")
    public RedisTemplate<String, Object> oauth2RedisTemplate() {
        

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer();

        
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(jsonSerializer);

        
        template.setDefaultSerializer(jsonSerializer);
        template.setEnableTransactionSupport(false);  

        template.afterPropertiesSet();

        
        return template;
    }

    



    @Bean
    @ConditionalOnProperty(name = "auth.redis.validation.enabled", havingValue = "true")
    public RedisConnectionValidator redisConnectionValidator() {
        
        return new RedisConnectionValidator(redisConnectionFactory);
    }

    



    @Bean
    public RedisHealthChecker redisHealthChecker() {
        

        return new RedisHealthChecker(oauth2MainRedisTemplate());
    }

    


    public static class RedisConnectionValidator {
        private final RedisConnectionFactory connectionFactory;

        public RedisConnectionValidator(RedisConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            validateConnection();
        }

        


        private void validateConnection() {
            

            try {
                
                var connection = connectionFactory.getConnection();
                if (connection != null) {
                    
                    connection.ping();
                    connection.close();

                    
                } else {
                    throw new IllegalStateException("鏃犳硶鑾峰彇Redis杩炴帴");
                }

            } catch (Exception e) {
                log.error("馃毃 Redis杩炴帴楠岃瘉澶辫触", e);
                throw new IllegalStateException("Redis杩炴帴楠岃瘉澶辫触", e);
            }
        }

        


        public String getConnectionInfo() {
            try {
                return String.format("Redis杩炴帴: %s, 鐘舵€? 姝ｅ父",
                        connectionFactory.getClass().getSimpleName());
            } catch (Exception e) {
                return String.format("Redis杩炴帴: %s, 鐘舵€? 寮傚父(%s)",
                        connectionFactory.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    


    public static class RedisHealthChecker {
        private final RedisTemplate<String, Object> redisTemplate;

        public RedisHealthChecker(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        


        public boolean isHealthy() {
            try {
                
                String testKey = "auth:health:check:" + System.currentTimeMillis();
                String testValue = "ok";

                
                redisTemplate.opsForValue().set(testKey, testValue,
                        java.time.Duration.ofSeconds(10));

                
                String result = (String) redisTemplate.opsForValue().get(testKey);

                
                redisTemplate.delete(testKey);

                boolean healthy = testValue.equals(result);

                if (healthy) {
                    log.debug("鉁?Redis鍋ュ悍妫€鏌ラ€氳繃");
                } else {
                    log.warn("Redis health check failed: inconsistent read/write verification result");
                }

                return healthy;

            } catch (Exception e) {
                log.error("Redis health check threw exception", e);
                return false;
            }
        }

        


        public RedisStats getStats() {
            try {
                
                return new RedisStats(true, "姝ｅ父", System.currentTimeMillis());
            } catch (Exception e) {
                return new RedisStats(false, e.getMessage(), System.currentTimeMillis());
            }
        }

        


        public static class RedisStats {
            private final boolean healthy;
            private final String status;
            private final long checkTime;

            public RedisStats(boolean healthy, String status, long checkTime) {
                this.healthy = healthy;
                this.status = status;
                this.checkTime = checkTime;
            }

            public boolean isHealthy() {
                return healthy;
            }

            public String getStatus() {
                return status;
            }

            public long getCheckTime() {
                return checkTime;
            }

            @Override
            public String toString() {
                return String.format("RedisStats{healthy=%s, status='%s', checkTime=%d}",
                        healthy, status, checkTime);
            }
        }
    }

    



    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class OAuth2AuthorizationMixin {
        
    }

    



    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class OAuth2AuthorizationConsentMixin {
        
    }
}
