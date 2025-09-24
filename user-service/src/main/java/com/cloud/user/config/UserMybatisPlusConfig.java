package com.cloud.user.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.cloud.common.config.MybatisPlusConfigFactory;
import com.cloud.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;

/**
 * 用户服务 MyBatis Plus 配置
 * 针对用户业务场景进行优化配置
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class UserMybatisPlusConfig {

    /**
     * 用户服务专用的MyBatis Plus拦截器配置
     * 使用标准配置，包含分页、乐观锁、防全表更新
     */
    @Bean
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("初始化用户服务MyBatis Plus拦截器配置");
        return MybatisPlusConfigFactory.createStandardInterceptor(DbType.MYSQL);
    }

    /**
     * 用户服务专用的元数据处理器
     * 扩展通用填充逻辑，特别适用于用户业务场景
     */
    @Bean
    @Primary
    public MetaObjectHandler metaObjectHandler() {
        log.info("初始化用户服务元数据处理器");
        return new UserMetaObjectHandler();
    }

    /**
     * 用户服务专用的元数据处理器实现
     */
    public static class UserMetaObjectHandler implements MetaObjectHandler {

        /**
         * 插入时自动填充
         */
        @Override
        public void insertFill(MetaObject metaObject) {
            log.debug("用户服务 - 开始插入填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());

            LocalDateTime now = LocalDateTime.now();
            Long currentUserId = getCurrentUserId();

            // 基础字段填充
            this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
            this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
            this.strictInsertFill(metaObject, "version", Integer.class, 1);

            // 操作人字段填充
            if (metaObject.hasGetter("createBy")) {
                this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);
            }
            if (metaObject.hasGetter("updateBy")) {
                this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
            }

            // 用户特有字段填充
            fillUserSpecificFields(metaObject);

            log.debug("用户服务 - 插入填充完成");
        }

        /**
         * 更新时自动填充
         */
        @Override
        public void updateFill(MetaObject metaObject) {
            log.debug("用户服务 - 开始更新填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());

            LocalDateTime now = LocalDateTime.now();
            Long currentUserId = getCurrentUserId();

            // 基础字段填充
            this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);

            // 操作人字段填充
            if (metaObject.hasGetter("updateBy")) {
                this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
            }

            log.debug("用户服务 - 更新填充完成");
        }

        /**
         * 填充用户特有字段
         */
        private void fillUserSpecificFields(MetaObject metaObject) {
            // 用户状态默认值
            if (metaObject.hasGetter("status") && metaObject.getValue("status") == null) {
                this.strictInsertFill(metaObject, "status", Integer.class, 1); // 1-正常
            }

            // 用户类型默认值
            if (metaObject.hasGetter("userType") && metaObject.getValue("userType") == null) {
                this.strictInsertFill(metaObject, "userType", String.class, "USER"); // USER-普通用户
            }

            // 注册来源默认值
            if (metaObject.hasGetter("registerSource") && metaObject.getValue("registerSource") == null) {
                this.strictInsertFill(metaObject, "registerSource", String.class, "WEB");
            }

            // 最后登录时间默认值
            if (metaObject.hasGetter("lastLoginTime") && metaObject.getValue("lastLoginTime") == null) {
                this.strictInsertFill(metaObject, "lastLoginTime", LocalDateTime.class, LocalDateTime.now());
            }
        }

        /**
         * 获取当前用户ID
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
