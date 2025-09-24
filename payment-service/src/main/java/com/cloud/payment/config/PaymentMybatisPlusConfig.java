package com.cloud.payment.config;

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
 * 支付服务 MyBatis Plus 配置
 * 针对支付业务场景进行优化配置
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class PaymentMybatisPlusConfig {

    /**
     * 支付服务专用的MyBatis Plus拦截器配置
     * 使用高并发场景配置，支付涉及资金安全需要乐观锁
     */
    @Bean
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("初始化支付服务MyBatis Plus拦截器配置");
        return MybatisPlusConfigFactory.createHighConcurrencyInterceptor(DbType.MYSQL);
    }

    /**
     * 支付服务专用的元数据处理器
     * 扩展通用填充逻辑，特别适用于支付业务场景
     */
    @Bean
    @Primary
    public MetaObjectHandler metaObjectHandler() {
        log.info("初始化支付服务元数据处理器");
        return new PaymentMetaObjectHandler();
    }

    /**
     * 支付服务专用的元数据处理器实现
     */
    public static class PaymentMetaObjectHandler implements MetaObjectHandler {

        /**
         * 插入时自动填充
         */
        @Override
        public void insertFill(MetaObject metaObject) {
            log.debug("支付服务 - 开始插入填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());

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

            // 支付特有字段填充
            fillPaymentSpecificFields(metaObject);

            log.debug("支付服务 - 插入填充完成");
        }

        /**
         * 更新时自动填充
         */
        @Override
        public void updateFill(MetaObject metaObject) {
            log.debug("支付服务 - 开始更新填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());

            LocalDateTime now = LocalDateTime.now();
            Long currentUserId = getCurrentUserId();

            // 基础字段填充
            this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);

            // 操作人字段填充
            if (metaObject.hasGetter("updateBy")) {
                this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
            }

            log.debug("支付服务 - 更新填充完成");
        }

        /**
         * 填充支付特有字段
         */
        private void fillPaymentSpecificFields(MetaObject metaObject) {
            // 支付状态默认值
            if (metaObject.hasGetter("paymentStatus") && metaObject.getValue("paymentStatus") == null) {
                this.strictInsertFill(metaObject, "paymentStatus", Integer.class, 0); // 0-待支付
            }

            // 支付方式默认值
            if (metaObject.hasGetter("paymentMethod") && metaObject.getValue("paymentMethod") == null) {
                this.strictInsertFill(metaObject, "paymentMethod", String.class, "ALIPAY");
            }

            // 支付渠道默认值
            if (metaObject.hasGetter("paymentChannel") && metaObject.getValue("paymentChannel") == null) {
                this.strictInsertFill(metaObject, "paymentChannel", String.class, "WEB");
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
