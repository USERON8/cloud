package com.cloud.common.config.base;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
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
public abstract class BaseRedisConfig {

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
}
