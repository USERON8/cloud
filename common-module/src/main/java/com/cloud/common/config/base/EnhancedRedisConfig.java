package com.cloud.common.config.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 增强的Redis配置基类
 * 提供优化的Redis配置，支持多种数据类型操作
 * 特别优化Hash类型使用，提升高频字段的读写性能
 *
 * @author what's up
 */
@Slf4j
public abstract class EnhancedRedisConfig extends BaseRedisConfig {

    /**
     * 创建优化的RedisTemplate配置
     * 针对不同数据类型进行序列化优化
     *
     * @param redisConnectionFactory Redis连接工厂
     * @return 配置好的RedisTemplate
     */
    @Override
    protected RedisTemplate<String, Object> createRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 使用StringRedisSerializer序列化key
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 使用Jackson序列化value，支持复杂对象
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 设置默认序列化器
        template.setDefaultSerializer(jsonSerializer);

        // 根据服务类型决定是否开启事务支持
        template.setEnableTransactionSupport(shouldEnableTransactionSupport());

        template.afterPropertiesSet();

        log.info("已创建优化的RedisTemplate配置");
        return template;
    }

    /**
     * 创建StringRedisTemplate
     * 用于简单字符串操作，性能更高
     *
     * @param redisConnectionFactory Redis连接工厂
     * @return StringRedisTemplate
     */
    protected StringRedisTemplate createStringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        template.afterPropertiesSet();

        log.info("已创建StringRedisTemplate配置");
        return template;
    }

    /**
     * 获取Hash操作接口
     * 优化高频字段的存储和查询性能
     *
     * @param redisTemplate RedisTemplate实例
     * @return HashOperations
     */
    protected HashOperations<String, String, Object> getHashOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForHash();
    }

    /**
     * 获取缓存键前缀
     * 子类必须实现，用于区分不同服务的缓存
     *
     * @return 缓存键前缀
     */
    protected abstract String getCacheKeyPrefix();

    /**
     * 构建完整的缓存键
     * 格式: service:type:key
     *
     * @param type 数据类型
     * @param key  具体键
     * @return 完整的缓存键
     */
    protected String buildCacheKey(String type, String key) {
        return String.format("%s:%s:%s", getCacheKeyPrefix(), type, key);
    }

    /**
     * 构建Hash类型的缓存键
     * 用于存储对象的多个字段
     *
     * @param type 数据类型
     * @param id   对象ID
     * @return Hash缓存键
     */
    protected String buildHashKey(String type, Object id) {
        return buildCacheKey(type, String.valueOf(id));
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

    /**
     * 是否启用缓存压缩
     * 对于大对象可以启用压缩以节省内存
     *
     * @return 是否启用压缩
     */
    protected boolean shouldEnableCompression() {
        return false;
    }

    /**
     * 获取缓存统计信息的键前缀
     *
     * @return 统计键前缀
     */
    protected String getStatsKeyPrefix() {
        return getCacheKeyPrefix() + ":stats";
    }
}
