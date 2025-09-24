package com.cloud.order.config;

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
 * 订单服务 MyBatis Plus 配置
 * 针对订单业务场景进行优化配置
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class OrderMybatisPlusConfig {

    /**
     * 订单服务专用的MyBatis Plus拦截器配置
     * 使用高并发场景配置，包含乐观锁等
     */
    @Bean
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("初始化订单服务MyBatis Plus拦截器配置");
        return MybatisPlusConfigFactory.createHighConcurrencyInterceptor(DbType.MYSQL);
    }

    /**
     * 订单服务专用的元数据处理器
     * 扩展通用填充逻辑，特别适用于订单业务场景
     */
    @Bean
    @Primary
    public MetaObjectHandler metaObjectHandler() {
        log.info("初始化订单服务元数据处理器");
        return new OrderMetaObjectHandler();
    }

    /**
     * 订单服务专用的元数据处理器实现
     */
    public static class OrderMetaObjectHandler implements MetaObjectHandler {

        /**
         * 插入时自动填充
         */
        @Override
        public void insertFill(MetaObject metaObject) {
            log.debug("订单服务 - 开始插入填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());

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

            // 订单特有字段填充
            fillOrderSpecificFields(metaObject, currentUserId);

            log.debug("订单服务 - 插入填充完成");
        }

        /**
         * 更新时自动填充
         */
        @Override
        public void updateFill(MetaObject metaObject) {
            log.debug("订单服务 - 开始更新填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());

            LocalDateTime now = LocalDateTime.now();
            Long currentUserId = getCurrentUserId();

            // 基础字段填充
            this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);

            // 操作人字段填充
            if (metaObject.hasGetter("updateBy")) {
                this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
            }

            log.debug("订单服务 - 更新填充完成");
        }

        /**
         * 填充订单特有字段
         */
        private void fillOrderSpecificFields(MetaObject metaObject, Long currentUserId) {
            // 订单状态默认值
            if (metaObject.hasGetter("orderStatus") && metaObject.getValue("orderStatus") == null) {
                this.strictInsertFill(metaObject, "orderStatus", Integer.class, 1); // 1-待支付
            }

            // 支付状态默认值
            if (metaObject.hasGetter("paymentStatus") && metaObject.getValue("paymentStatus") == null) {
                this.strictInsertFill(metaObject, "paymentStatus", Integer.class, 0); // 0-未支付
            }

            // 订单来源默认值
            if (metaObject.hasGetter("orderSource") && metaObject.getValue("orderSource") == null) {
                this.strictInsertFill(metaObject, "orderSource", String.class, "WEB");
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
