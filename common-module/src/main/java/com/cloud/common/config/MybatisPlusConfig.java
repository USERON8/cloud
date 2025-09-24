package com.cloud.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.cloud.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 通用配置类
 * 提供默认的MyBatis Plus配置，子服务可以通过继承或重写来个性化配置
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis Plus 拦截器配置
     * 包含分页插件、乐观锁插件、防全表更新插件
     * 子服务可以通过@Primary注解覆盖此配置
     */
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("初始化MyBatis Plus默认拦截器配置");
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件 - 默认MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 防全表更新插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    /**
     * 默认的元数据处理器
     * 自动填充创建时间、更新时间、版本号等字段
     * 子服务可以通过@Primary注解覆盖此配置
     */
    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MetaObjectHandler metaObjectHandler() {
        log.info("初始化MyBatis Plus默认元数据处理器");
        return new DefaultMetaObjectHandler();
    }

    /**
     * 默认元数据处理器实现
     * 提供通用的字段自动填充功能
     */
    public static class DefaultMetaObjectHandler implements MetaObjectHandler {

        /**
         * 插入时自动填充
         */
        @Override
        public void insertFill(MetaObject metaObject) {
            log.debug("开始插入填充 - 实体类: {}", metaObject.getOriginalObject().getClass().getSimpleName());

            LocalDateTime now = LocalDateTime.now();

            // 填充创建时间
            this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);

            // 填充更新时间
            this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);

            // 填充版本号（乐观锁）
            this.strictInsertFill(metaObject, "version", Integer.class, 1);

            // 填充创建人（如果字段存在）
            if (metaObject.hasGetter("createBy")) {
                Long currentUserId = getCurrentUserId();
                this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);
            }

            // 填充更新人（如果字段存在）
            if (metaObject.hasGetter("updateBy")) {
                Long currentUserId = getCurrentUserId();
                this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
            }

            log.debug("插入填充完成 - createdAt: {}, updatedAt: {}", now, now);
        }

        /**
         * 更新时自动填充
         */
        @Override
        public void updateFill(MetaObject metaObject) {
            log.debug("开始更新填充 - 实体类: {}", metaObject.getOriginalObject().getClass().getSimpleName());

            LocalDateTime now = LocalDateTime.now();

            // 填充更新时间
            this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);

            // 填充更新人（如果字段存在）
            if (metaObject.hasGetter("updateBy")) {
                Long currentUserId = getCurrentUserId();
                this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
            }

            log.debug("更新填充完成 - updatedAt: {}", now);
        }

        /**
         * 获取当前用户ID
         * 优先从SecurityUtils获取，如果获取失败则返回系统用户ID
         */
        private Long getCurrentUserId() {
            try {
                return SecurityUtils.getCurrentUserId();
            } catch (Exception e) {
                log.debug("获取当前用户ID失败，使用系统用户ID: {}", e.getMessage());
                return 0L; // 系统用户ID
            }
        }
    }
}
