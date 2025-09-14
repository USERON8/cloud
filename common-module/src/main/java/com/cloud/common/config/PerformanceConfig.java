package com.cloud.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.sql.DataSource;
import java.time.Duration;

/**
 * 性能优化配置类
 * 包含连接池调优、缓存策略优化等配置
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class PerformanceConfig {

    @Value("${spring.datasource.url:}")
    private String jdbcUrl;

    @Value("${spring.datasource.username:}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${spring.datasource.driver-class-name:}")
    private String driverClassName;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * 优化的HikariCP数据源配置
     * 针对高并发场景进行调优
     */
    @Bean
    @Primary
    @ConditionalOnClass(HikariDataSource.class)
    @ConditionalOnProperty(name = "performance.datasource.optimized.enabled", havingValue = "true", matchIfMissing = false)
    public DataSource optimizedDataSource() {
        // 检查必须的数据库配置
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty() ||
                driverClassName == null || driverClassName.trim().isEmpty()) {
            log.warn("数据库配置不完整，跳过数据源创建");
            return null;
        }

        HikariConfig config = new HikariConfig();

        // 基本连接信息
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // 连接池优化配置
        config.setMaximumPoolSize(20);           // 最大连接数（根据CPU核心数 * 2-4）
        config.setMinimumIdle(5);                // 最小空闲连接数
        config.setConnectionTimeout(30000);      // 连接超时时间 30秒
        config.setIdleTimeout(600000);           // 空闲超时时间 10分钟
        config.setMaxLifetime(1800000);          // 连接最大生存时间 30分钟
        config.setLeakDetectionThreshold(60000); // 连接泄露检测阈值 1分钟

        // 性能优化参数
        config.setAutoCommit(true);              // 自动提交
        config.setIsolateInternalQueries(true);  // 隔离内部查询
        config.setAllowPoolSuspension(false);    // 不允许暂停连接池

        // 连接验证
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);       // 验证超时 5秒

        // 连接池名称
        config.setPoolName("OptimizedHikariCP");

        // JMX监控
        config.setRegisterMbeans(true);

        log.info("初始化优化的HikariCP数据源，最大连接数: {}, 最小空闲连接: {}",
                config.getMaximumPoolSize(), config.getMinimumIdle());

        return new HikariDataSource(config);
    }

    /**
     * 优化的Redis连接工厂
     * 使用Lettuce连接池进行调优
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "performance.redis.optimized.enabled", havingValue = "true", matchIfMissing = true)
    public RedisConnectionFactory optimizedRedisConnectionFactory() {

        // Redis服务器配置
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisHost);
        serverConfig.setPort(redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            serverConfig.setPassword(redisPassword);
        }
        serverConfig.setDatabase(redisDatabase);

        // 连接池配置
        @SuppressWarnings("rawtypes")
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);              // 最大连接数
        poolConfig.setMaxIdle(8);                // 最大空闲连接数
        poolConfig.setMinIdle(2);                // 最小空闲连接数
        poolConfig.setMaxWait(Duration.ofSeconds(10)); // 最大等待时间
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMinutes(1)); // 清理间隔
        poolConfig.setMinEvictableIdleTime(Duration.ofMinutes(5));    // 最小空闲时间
        poolConfig.setTestOnBorrow(true);        // 获取连接时测试
        poolConfig.setTestOnReturn(false);       // 归还连接时不测试
        poolConfig.setTestWhileIdle(true);       // 空闲时测试连接
        poolConfig.setNumTestsPerEvictionRun(3); // 每次清理测试的连接数
        poolConfig.setBlockWhenExhausted(true);  // 连接耗尽时阻塞

        // Lettuce客户端配置
        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .commandTimeout(Duration.ofSeconds(5))     // 命令超时时间
                .shutdownTimeout(Duration.ofSeconds(3))    // 关闭超时时间
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(serverConfig, clientConfig);
        factory.setValidateConnection(true);  // 验证连接
        factory.setShareNativeConnection(false); // 不共享原生连接（提高并发性能）

        log.info("初始化优化的Redis连接工厂，最大连接数: {}, 最大空闲连接: {}",
                poolConfig.getMaxTotal(), poolConfig.getMaxIdle());

        return factory;
    }

    /**
     * 高性能RedisTemplate配置
     * 优化序列化器和连接配置
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "performance.redis.template.optimized.enabled", havingValue = "true", matchIfMissing = true)
    public RedisTemplate<String, Object> optimizedRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 使用高性能序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 使用JSON序列化器，性能比JDK序列化器更好
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());

        // 设置默认序列化器
        template.setDefaultSerializer(RedisSerializer.json());

        // 开启事务支持
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();

        log.info("初始化高性能RedisTemplate，使用JSON序列化器");

        return template;
    }
}
