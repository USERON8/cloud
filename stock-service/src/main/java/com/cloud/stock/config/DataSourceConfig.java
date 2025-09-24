package com.cloud.stock.config;

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
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDateTime;

/**
 * 库存服务 MyBatis Plus 配置
 * 针对库存业务场景进行优化配置，特别启用乐观锁插件
 * 适用于高并发库存扣减场景
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableTransactionManagement
public class DataSourceConfig {

    /**
     * 库存服务专用的MyBatis Plus拦截器配置
     * 使用高并发场景配置，必须启用乐观锁
     */
    @Bean
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("初始化库存服务MyBatis Plus拦截器，启用乐观锁支持");
        return MybatisPlusConfigFactory.createHighConcurrencyInterceptor(DbType.MYSQL);
    }

    /**
     * 库存服务专用的元数据处理器
     * 自动填充创建时间、更新时间等字段
     */
    @Bean
    @Primary
    public MetaObjectHandler metaObjectHandler() {
        log.info("初始化库存服务元数据处理器");
        return new StockMetaObjectHandler();
    }

    /**
     * 库存服务专用的元数据处理器实现
     * 扩展通用填充逻辑，特别适用于库存业务场景
     */
    public static class StockMetaObjectHandler implements MetaObjectHandler {

        /**
         * 插入时自动填充
         */
        @Override
        public void insertFill(MetaObject metaObject) {
            log.debug("库存服务 - 开始插入填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());

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

            // 库存特有字段填充
            fillStockSpecificFields(metaObject);

            log.debug("库存服务 - 插入填充完成");
        }

        /**
         * 更新时自动填充
         */
        @Override
        public void updateFill(MetaObject metaObject) {
            log.debug("库存服务 - 开始更新填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());

            LocalDateTime now = LocalDateTime.now();
            Long currentUserId = getCurrentUserId();

            // 基础字段填充
            this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);

            // 操作人字段填充
            if (metaObject.hasGetter("updateBy")) {
                this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
            }

            log.debug("库存服务 - 更新填充完成");
        }

        /**
         * 填充库存特有字段
         */
        private void fillStockSpecificFields(MetaObject metaObject) {
            // 库存状态默认值
            if (metaObject.hasGetter("stockStatus") && metaObject.getValue("stockStatus") == null) {
                this.strictInsertFill(metaObject, "stockStatus", Integer.class, 1); // 1-正常
            }

            // 冻结库存默认值
            if (metaObject.hasGetter("frozenQuantity") && metaObject.getValue("frozenQuantity") == null) {
                this.strictInsertFill(metaObject, "frozenQuantity", Integer.class, 0);
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
