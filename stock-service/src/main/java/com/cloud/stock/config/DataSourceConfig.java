package com.cloud.stock.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.cloud.common.config.base.BaseDataSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 库存服务数据源配置
 * 继承BaseDataSourceConfig，启用MyBatis-Plus插件和事务管理
 * 特别启用乐观锁插件，适用于高并发库存扣减场景
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableTransactionManagement
public class DataSourceConfig extends BaseDataSourceConfig {

    /**
     * 配置MyBatis-Plus拦截器
     * 包括分页插件、乐观锁插件、防全表更新插件
     * 库存服务特别需要乐观锁来处理并发扣减
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("初始化库存服务MyBatis-Plus拦截器，启用乐观锁支持");
        return createMybatisPlusInterceptor();
    }

    /**
     * 配置元数据处理器
     * 自动填充创建时间、更新时间等字段
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new StockMetaObjectHandler();
    }

    /**
     * 库存服务确保启用乐观锁插件
     * 用于高并发库存扣减场景
     */
    @Override
    protected boolean shouldEnableOptimisticLocker() {
        return true; // 库存服务必须启用乐观锁
    }

    /**
     * 库存服务专用的元数据处理器
     * 扩展通用填充逻辑，特别适用于库存业务场景
     */
    public static class StockMetaObjectHandler extends BaseDataSourceConfig {

        @Override
        protected void doInsertFill(MetaObject metaObject) {
            // 库存服务特有的插入填充逻辑
            if (metaObject.hasGetter("createBy")) {
                this.strictInsertFill(metaObject, "createBy", Long.class, getCurrentUserId());
            }

            // 库存特有字段填充
            if (metaObject.hasGetter("version")) {
                this.strictInsertFill(metaObject, "version", Integer.class, 1);
            }
        }

        @Override
        protected void doUpdateFill(MetaObject metaObject) {
            // 库存服务特有的更新填充逻辑
            if (metaObject.hasGetter("updateBy")) {
                this.strictUpdateFill(metaObject, "updateBy", Long.class, getCurrentUserId());
            }
        }

        /**
         * 获取当前用户ID
         * 从SecurityContext或RequestContext中获取
         */
        private Long getCurrentUserId() {
            // TODO: 从安全上下文获取当前用户ID
            // return SecurityUtils.getCurrentUserId();
            return null; // 暂时返回null
        }
    }
}
