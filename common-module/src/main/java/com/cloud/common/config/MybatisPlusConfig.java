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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 统一配置类
 * <p>
 * 主要功能：
 * - 统一的MyBatis Plus拦截器配置（分页、乐观锁、防全表更新）
 * - 通用的元数据自动填充处理器
 * - 支持创建时间、更新时间、版本号、操作人等字段的自动填充
 * - 集成Spring Security获取当前用户信息
 *
 * @author CloudDevAgent
 * @version 2.0
 * @since 2025-09-27
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis Plus 统一拦截器配置
     * <p>
     * 包含功能：
     * - 分页插件：支持MySQL分页查询
     * - 乐观锁插件：防止并发更新冲突
     * - 防全表更新插件：防止误操作全表数据
     * <p>
     * 使用@Primary注解确保优先使用此配置
     */
    @Bean
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("初始化MyBatis Plus统一拦截器配置");
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 分页插件 - 默认MySQL，支持多种数据库
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(1000L); // 设置最大单页限制
        paginationInterceptor.setOverflow(false); // 禁止超过总数后返回空结果
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 2. 乐观锁插件 - 防止并发更新冲突
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 3. 防全表更新插件 - 防止误操作
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    /**
     * MyBatis Plus 统一元数据处理器
     * <p>
     * 主要功能：
     * - 自动填充创建时间（createdAt）、更新时间（updatedAt）
     * - 自动填充版本号（version）用于乐观锁
     * - 自动填充创建人（createBy）、更新人（updateBy）
     * - 集成Spring Security获取当前用户信息
     * <p>
     * 使用@Primary注解确保优先使用此配置
     */
    @Bean
    @Primary
    public MetaObjectHandler metaObjectHandler() {
        log.info("初始化MyBatis Plus统一元数据处理器");
        return new UnifiedMetaObjectHandler();
    }

    /**
     * 统一元数据处理器实现
     * <p>
     * 提供全平台统一的字段自动填充功能，支持：
     * - 标准时间字段：createdAt, updatedAt
     * - 乐观锁字段：version
     * - 操作人字段：createBy, updateBy
     * - 兼容多种命名风格：驼峰命名和下划线命名
     */
    public static class UnifiedMetaObjectHandler implements MetaObjectHandler {

        /**
         * 插入时自动填充
         * <p>
         * 支持多种字段命名风格：
         * - 驼峰命名：createdAt, updatedAt, createBy, updateBy
         * - 下划线命名：created_at, updated_at, create_by, update_by
         */
        @Override
        public void insertFill(MetaObject metaObject) {
            log.debug("开始插入填充 - 实体类: {}", metaObject.getOriginalObject().getClass().getSimpleName());

            LocalDateTime now = LocalDateTime.now();
            Long currentUserId = getCurrentUserId();

            // 1. 填充创建时间（支持多种命名）
            fillDateTimeField(metaObject, new String[]{"createdAt", "created_at", "createTime", "create_time"}, now);

            // 2. 填充更新时间（支持多种命名）
            fillDateTimeField(metaObject, new String[]{"updatedAt", "updated_at", "updateTime", "update_time"}, now);

            // 3. 填充版本号（乐观锁）
            fillField(metaObject, new String[]{"version"}, Integer.class, 1);

            // 4. 填充创建人
            fillField(metaObject, new String[]{"createBy", "create_by", "createdBy", "created_by"}, Long.class, currentUserId);

            // 5. 填充更新人
            fillField(metaObject, new String[]{"updateBy", "update_by", "updatedBy", "updated_by"}, Long.class, currentUserId);

            // 6. 填充逻辑删除标记（如果存在）
            fillField(metaObject, new String[]{"deleted", "is_deleted"}, Integer.class, 0);

            log.debug("插入填充完成 - createdAt: {}, updatedAt: {}, userId: {}", now, now, currentUserId);
        }

        /**
         * 更新时自动填充
         * <p>
         * 自动填充更新时间和更新人信息
         */
        @Override
        public void updateFill(MetaObject metaObject) {
            log.debug("开始更新填充 - 实体类: {}", metaObject.getOriginalObject().getClass().getSimpleName());

            LocalDateTime now = LocalDateTime.now();
            Long currentUserId = getCurrentUserId();

            // 1. 填充更新时间（支持多种命名）
            updateDateTimeField(metaObject, new String[]{"updatedAt", "updated_at", "updateTime", "update_time"}, now);

            // 2. 填充更新人（支持多种命名）
            updateField(metaObject, new String[]{"updateBy", "update_by", "updatedBy", "updated_by"}, Long.class, currentUserId);

            log.debug("更新填充完成 - updatedAt: {}, userId: {}", now, currentUserId);
        }

        /**
         * 通用字段填充方法（插入时）
         */
        private <T> void fillField(MetaObject metaObject, String[] fieldNames, Class<T> fieldType, T value) {
            for (String fieldName : fieldNames) {
                if (metaObject.hasGetter(fieldName)) {
                    this.strictInsertFill(metaObject, fieldName, fieldType, value);
                    log.debug("填充字段: {} = {}", fieldName, value);
                    break; // 只填充第一个匹配的字段
                }
            }
        }

        /**
         * 通用时间字段填充方法（插入时）
         */
        private void fillDateTimeField(MetaObject metaObject, String[] fieldNames, LocalDateTime value) {
            fillField(metaObject, fieldNames, LocalDateTime.class, value);
        }

        /**
         * 通用字段更新方法（更新时）
         */
        private <T> void updateField(MetaObject metaObject, String[] fieldNames, Class<T> fieldType, T value) {
            for (String fieldName : fieldNames) {
                if (metaObject.hasGetter(fieldName)) {
                    this.strictUpdateFill(metaObject, fieldName, fieldType, value);
                    log.debug("更新字段: {} = {}", fieldName, value);
                    break; // 只更新第一个匹配的字段
                }
            }
        }

        /**
         * 通用时间字段更新方法（更新时）
         */
        private void updateDateTimeField(MetaObject metaObject, String[] fieldNames, LocalDateTime value) {
            updateField(metaObject, fieldNames, LocalDateTime.class, value);
        }

        /**
         * 获取当前用户ID
         * <p>
         * 优先从 SecurityUtils 获取当前登录用户ID，
         * 如果获取失败（未登录或系统初始化时）则返回系统用户ID
         */
        private Long getCurrentUserId() {
            try {
                Long userId = SecurityUtils.getCurrentUserId();
                return userId != null ? userId : 0L;
            } catch (Exception e) {
                log.debug("获取当前用户ID失败，使用系统用户ID: {}", e.getMessage());
                return 0L; // 系统用户ID
            }
        }
    }
}
