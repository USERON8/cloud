package com.cloud.common.cache.config;

import com.cloud.common.cache.core.MultiLevelCacheManager;
import com.cloud.common.cache.metrics.CacheMetricsCollector;
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

/**
 * 统一缓存配置工厂
 * <p>
 * 根据配置自动选择单Redis缓存或多级缓存的智能切换机制。
 * 支持条件化配置，可以灵活地在不同环境下使用不同的缓存策略。
 *
 * @author CloudDevAgent
 * @version 2.0
 * @since 2025-09-26
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfigFactory {

    private final RedisConnectionFactory redisConnectionFactory;

    // 使用ApplicationContext获取来避免循环依赖
    private final org.springframework.context.ApplicationContext applicationContext;

    /**
     * 多级缓存管理器
     * <p>
     * 当配置项cache.multi-level=true时启用
     * 提供Caffeine + Redis双级缓存功能
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "cache", name = "multi-level", havingValue = "true")
    public CacheManager multiLevelCacheManager() {
        // 创建RedisTemplate
        RedisTemplate<String, Object> redisTemplate = createCacheRedisTemplate();

        // 创建缓存配置
        MultiLevelCacheManager.MultiLevelCacheConfig config = createMultiLevelConfig();

        // 生成节点ID
        String nodeId = generateNodeId();

        // 创建多级缓存管理器，不传递 CacheMetricsCollector，完全避免循环依赖
        MultiLevelCacheManager cacheManager = new MultiLevelCacheManager(
                redisTemplate,
                config,
                nodeId
        );

        log.info("🚀 启用多级缓存管理器: nodeId={}, keyPrefix={}, defaultExpire={}s",
                nodeId, config.getKeyPrefix(), config.getDefaultExpireSeconds());

        return cacheManager;
    }

    /**
     * 缓存指标收集器初始化后的回调
     * 用于在CacheMetricsCollector创建后将其设置到MultiLevelCacheManager中
     */
    @Bean
    @ConditionalOnProperty(prefix = "cache", name = "multi-level", havingValue = "true")
    public org.springframework.beans.factory.config.BeanPostProcessor cacheMetricsPostProcessor() {
        return new org.springframework.beans.factory.config.BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof CacheMetricsCollector metricsCollector) {
                    // 在CacheMetricsCollector初始化后，将其设置到已存在的MultiLevelCacheManager中
                    try {
                        CacheManager cacheManager = applicationContext.getBean(CacheManager.class);
                        if (cacheManager instanceof MultiLevelCacheManager multiLevelCacheManager) {
                            multiLevelCacheManager.setMetricsCollector(metricsCollector);
                            log.info("✅ 缓存指标收集器已设置到多级缓存管理器");
                        }
                    } catch (Exception e) {
                        log.warn("设置缓存指标收集器失败: {}", e.getMessage());
                    }
                }
                return bean;
            }
        };
    }

    /**
     * 单Redis缓存管理器
     * <p>
     * 当配置项cache.multi-level=false或未配置时启用
     * 提供标准的Redis缓存功能
     */
    @Bean("standardRedisCacheManager")
    @ConditionalOnProperty(prefix = "cache", name = "multi-level", havingValue = "false", matchIfMissing = true)
    public CacheManager standardRedisCacheManager() {
        // Redis缓存配置
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(1800)) // 30分钟过期
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues(); // 不缓存null值

        // 创建Redis缓存管理器
        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .build();

        log.info("🔧 启用标准Redis缓存管理器: ttl=1800s, nullValues=false");

        return cacheManager;
    }

    /**
     * 创建缓存专用的Redis操作模板
     * <p>
     * 配置键值序列化方式，用于多级缓存的Redis操作
     */
    private RedisTemplate<String, Object> createCacheRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 设置序列化方式
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();

        log.debug("配置缓存RedisTemplate: keySerializer=String, valueSerializer=Jackson2Json");

        return template;
    }

    /**
     * Redis消息监听容器
     *
     * 用于Redis Pub/Sub缓存一致性消息监听
     * 注意：暂时禁用以避免循环依赖，后续通过事件机制实现
     */
    // @Bean
    // @ConditionalOnProperty(prefix = "cache", name = "multi-level", havingValue = "true")
    // public RedisMessageListenerContainer cacheMessageListenerContainer(
    //         CacheMessageListener cacheMessageListener) {
    //     
    //     RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    //     container.setConnectionFactory(redisConnectionFactory);
    //     
    //     // 订阅缓存一致性消息主题
    //     container.addMessageListener(cacheMessageListener, new PatternTopic("cache:message"));
    //     
    //     log.info("配置Redis消息监听容器: topic=cache:message");
    //     
    //     return container;
    // }

    /**
     * JSON对象映射器
     *
     * 用于缓存消息的序列化和反序列化
     * 暂时禁用以避免循环依赖
     */
    // @Bean
    // @ConditionalOnProperty(prefix = "cache", name = "multi-level", havingValue = "true")
    // public ObjectMapper cacheObjectMapper() {
    //     ObjectMapper mapper = new ObjectMapper();
    //     // 配置时间序列化格式等
    //     mapper.findAndRegisterModules();
    //     
    //     log.debug("配置缓存JSON映射器");
    //     
    //     return mapper;
    // }

    /**
     * 创建多级缓存配置
     *
     * @return 多级缓存配置对象
     */
    private MultiLevelCacheManager.MultiLevelCacheConfig createMultiLevelConfig() {
        MultiLevelCacheManager.MultiLevelCacheConfig config = new MultiLevelCacheManager.MultiLevelCacheConfig();

        // 设置默认值，可以通过配置文件覆盖
        config.setDefaultExpireSeconds(1800); // 30分钟
        config.setKeyPrefix("cache:");
        config.setMessageTopic("cache:message");
        config.setAllowNullValues(false); // 不允许null值，避免缓存穿透

        // Caffeine本地缓存配置
        MultiLevelCacheManager.CaffeineConfig caffeineConfig = new MultiLevelCacheManager.CaffeineConfig();
        caffeineConfig.setMaximumSize(1000L);
        caffeineConfig.setExpireAfterWriteMinutes(30);
        caffeineConfig.setExpireAfterAccessMinutes(10);
        caffeineConfig.setRecordStats(true);

        config.setCaffeineConfig(caffeineConfig);

        return config;
    }

    /**
     * 生成唯一的节点ID
     * <p>
     * 格式：hostname-timestamp-pid
     * 用于标识当前节点，避免处理自己发送的缓存消息
     *
     * @return 节点ID
     */
    private String generateNodeId() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String pid = String.valueOf(ProcessHandle.current().pid());
            long timestamp = System.currentTimeMillis();

            return String.format("%s-%d-%s", hostname, timestamp, pid);
        } catch (UnknownHostException e) {
            // 如果获取主机名失败，使用时间戳和随机数
            long timestamp = System.currentTimeMillis();
            int random = (int) (Math.random() * 10000);
            String nodeId = String.format("node-%d-%d", timestamp, random);

            log.warn("无法获取主机名，使用随机节点ID: {}", nodeId);
            return nodeId;
        }
    }

    /**
     * 缓存配置属性类（用于配置绑定）
     */
    public static class CacheProperties {
        /**
         * 是否启用多级缓存
         */
        private boolean multiLevel = false;

        /**
         * 默认过期时间（秒）
         */
        private long defaultExpireSeconds = 1800;

        /**
         * Redis键前缀
         */
        private String keyPrefix = "cache:";

        /**
         * 缓存消息主题
         */
        private String messageTopic = "cache:message";

        // Getters and Setters
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
