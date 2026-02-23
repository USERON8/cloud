package com.cloud.common.cache.config;

import com.cloud.common.cache.core.MultiLevelCacheManager;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.cloud.common.cache.metrics.CacheMetricsCollector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;











@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfigFactory {

    private final RedisConnectionFactory redisConnectionFactory;

    
    private final org.springframework.context.ApplicationContext applicationContext;

    





    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "cache", name = "multi-level", havingValue = "true")
    public CacheManager multiLevelCacheManager() {
        
        RedisTemplate<String, Object> redisTemplate = createCacheRedisTemplate();

        
        MultiLevelCacheManager.MultiLevelCacheConfig config = createMultiLevelConfig();

        
        String nodeId = generateNodeId();

        
        MultiLevelCacheManager cacheManager = new MultiLevelCacheManager(
                redisTemplate,
                config,
                nodeId
        );

        


        return cacheManager;
    }

    



    @Bean
    @ConditionalOnProperty(prefix = "cache", name = "multi-level", havingValue = "true")
    public org.springframework.beans.factory.config.BeanPostProcessor cacheMetricsPostProcessor() {
        return new org.springframework.beans.factory.config.BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof CacheMetricsCollector metricsCollector) {
                    
                    try {
                        CacheManager cacheManager = applicationContext.getBean(CacheManager.class);
                        if (cacheManager instanceof MultiLevelCacheManager multiLevelCacheManager) {
                            multiLevelCacheManager.setMetricsCollector(metricsCollector);
                            
                        }
                    } catch (Exception e) {
                        log.warn("璁剧疆缂撳瓨鎸囨爣鏀堕泦鍣ㄥけ璐? {}", e.getMessage());
                    }
                }
                return bean;
            }
        };
    }

    





    @Bean("standardRedisCacheManager")
    @ConditionalOnProperty(prefix = "cache", name = "multi-level", havingValue = "false", matchIfMissing = true)
    public CacheManager standardRedisCacheManager() {
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(1800)) 
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues(); 

        
        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .build();

        

        return cacheManager;
    }

    




    private RedisTemplate<String, Object> createCacheRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();

        log.debug("閰嶇疆缂撳瓨RedisTemplate: keySerializer=String, valueSerializer=Jackson2Json");

        return template;
    }

    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        objectMapper.findAndRegisterModules();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    





    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    





    
    
    
    
    
    
    
    
    
    
    

    




    private MultiLevelCacheManager.MultiLevelCacheConfig createMultiLevelConfig() {
        MultiLevelCacheManager.MultiLevelCacheConfig config = new MultiLevelCacheManager.MultiLevelCacheConfig();

        
        config.setDefaultExpireSeconds(1800); 
        config.setKeyPrefix("cache:");
        config.setMessageTopic("cache:message");
        config.setAllowNullValues(false); 

        
        MultiLevelCacheManager.CaffeineConfig caffeineConfig = new MultiLevelCacheManager.CaffeineConfig();
        caffeineConfig.setMaximumSize(1000L);
        caffeineConfig.setExpireAfterWriteMinutes(30);
        caffeineConfig.setExpireAfterAccessMinutes(10);
        caffeineConfig.setRecordStats(true);

        config.setCaffeineConfig(caffeineConfig);

        return config;
    }

    







    private String generateNodeId() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String pid = String.valueOf(ProcessHandle.current().pid());
            long timestamp = System.currentTimeMillis();

            return String.format("%s-%d-%s", hostname, timestamp, pid);
        } catch (UnknownHostException e) {
            
            long timestamp = System.currentTimeMillis();
            int random = (int) (Math.random() * 10000);
            String nodeId = String.format("node-%d-%d", timestamp, random);

            log.warn("鏃犳硶鑾峰彇涓绘満鍚嶏紝浣跨敤闅忔満鑺傜偣ID: {}", nodeId);
            return nodeId;
        }
    }

    


    public static class CacheProperties {
        


        private boolean multiLevel = false;

        


        private long defaultExpireSeconds = 1800;

        


        private String keyPrefix = "cache:";

        


        private String messageTopic = "cache:message";

        
        public boolean isMultiLevel() {
            return multiLevel;
        }

        public void setMultiLevel(boolean multiLevel) {
            this.multiLevel = multiLevel;
        }

        public long getDefaultExpireSeconds() {
            return defaultExpireSeconds;
        }

        public void setDefaultExpireSeconds(long defaultExpireSeconds) {
            this.defaultExpireSeconds = defaultExpireSeconds;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public String getMessageTopic() {
            return messageTopic;
        }

        public void setMessageTopic(String messageTopic) {
            this.messageTopic = messageTopic;
        }
    }
}
