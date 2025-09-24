package com.cloud.common.config.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 基础Redis配置抽象类
 * 提供Redis基本配置模板，各服务按需继承和扩展
 * <p>
 * 遵循服务自治原则：
 * - common-module只提供基础模板
 * - 具体服务自行决定是否启用和如何配置Redis
 *
 * @author what's up
 */
@Slf4j
public abstract class BaseRedisConfig {

    /**
     * 创建标准的RedisTemplate配置
     * 子类可以通过@Primary注解覆盖此配置
     *
     * @param redisConnectionFactory Redis连接工厂
     * @return 配置好的RedisTemplate
     */
    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("初始化Redis模板配置");
        return createRedisTemplate(redisConnectionFactory);
    }

    /**
     * 创建StringRedisTemplate
     * 用于简单的字符串操作
     *
     * @param redisConnectionFactory Redis连接工厂
     * @return StringRedisTemplate
     */
    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("初始化StringRedisTemplate配置");
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        template.setEnableTransactionSupport(shouldEnableTransactionSupport());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建标准的RedisTemplate配置
     * 子类可以重写此方法来自定义配置
     *
     * @param redisConnectionFactory Redis连接工厂
     * @return 配置好的RedisTemplate
     */
    protected RedisTemplate<String, Object> createRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());

        // 设置默认序列化器
        template.setDefaultSerializer(RedisSerializer.json());

        // 子类可以决定是否开启事务支持
        template.setEnableTransactionSupport(shouldEnableTransactionSupport());

        // 初始化RedisTemplate
        template.afterPropertiesSet();

        return template;
    }

    /**
     * 子类可以重写此方法来决定是否启用事务支持
     * 默认不启用，因为大多数缓存场景不需要事务
     *
     * @return 是否启用事务支持
     */
    protected boolean shouldEnableTransactionSupport() {
        return false;
    }

    /**
     * 获取服务名称前缀
     * 子类可以重写此方法来自定义缓存键前缀
     *
     * @return 服务名称前缀
     */
    protected String getServicePrefix() {
        return "default";
    }

    /**
     * 构建缓存键
     * 格式: service:type:key
     *
     * @param type 数据类型
     * @param key  具体键
     * @return 完整的缓存键
     */
    protected String buildCacheKey(String type, String key) {
        return String.format("%s:%s:%s", getServicePrefix(), type, key);
    }

    /**
     * 获取缓存过期时间（秒）
     * 子类可重写以定制不同类型数据的过期策略
     *
     * @param type 数据类型
     * @return 过期时间（秒）
     */
    protected long getCacheExpireTime(String type) {
        // 默认1小时过期
        return 3600L;
    }
}
