package com.cloud.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import lombok.extern.slf4j.Slf4j;

/**
 * MyBatis Plus 配置工厂类
 * 提供各种预定义的配置模板，子服务可以根据需要选择使用
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
public class MybatisPlusConfigFactory {

    /**
     * 创建基础拦截器配置
     * 包含：分页插件
     *
     * @param dbType 数据库类型
     * @return 拦截器
     */
    public static MybatisPlusInterceptor createBasicInterceptor(DbType dbType) {
        log.info("创建基础MyBatis Plus拦截器 - 数据库类型: {}", dbType);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));

        return interceptor;
    }

    /**
     * 创建标准拦截器配置
     * 包含：分页插件、乐观锁插件、防全表更新插件
     *
     * @param dbType 数据库类型
     * @return 拦截器
     */
    public static MybatisPlusInterceptor createStandardInterceptor(DbType dbType) {
        log.info("创建标准MyBatis Plus拦截器 - 数据库类型: {}", dbType);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));

        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 防全表更新插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    /**
     * 创建高并发场景拦截器配置
     * 包含：分页插件、乐观锁插件、防全表更新插件、数据权限插件
     * 适用于库存服务、订单服务等高并发场景
     *
     * @param dbType 数据库类型
     * @return 拦截器
     */
    public static MybatisPlusInterceptor createHighConcurrencyInterceptor(DbType dbType) {
        log.info("创建高并发MyBatis Plus拦截器 - 数据库类型: {}", dbType);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(dbType);
        paginationInterceptor.setMaxLimit(1000L); // 限制最大查询数量
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 乐观锁插件（高并发场景必须）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 防全表更新插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    /**
     * 创建多租户拦截器配置
     * 包含：分页插件、乐观锁插件、防全表更新插件、多租户插件
     * 适用于需要数据隔离的场景
     *
     * @param dbType            数据库类型
     * @param tenantLineHandler 租户处理器
     * @return 拦截器
     */
    public static MybatisPlusInterceptor createMultiTenantInterceptor(DbType dbType,
                                                                      TenantLineHandler tenantLineHandler) {
        log.info("创建多租户MyBatis Plus拦截器 - 数据库类型: {}", dbType);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 多租户插件（必须放在第一个）
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(tenantLineHandler);
        interceptor.addInnerInterceptor(tenantInterceptor);

        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));

        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 防全表更新插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    /**
     * 创建只读服务拦截器配置
     * 包含：分页插件
     * 适用于查询服务、搜索服务等只读场景
     *
     * @param dbType 数据库类型
     * @return 拦截器
     */
    public static MybatisPlusInterceptor createReadOnlyInterceptor(DbType dbType) {
        log.info("创建只读MyBatis Plus拦截器 - 数据库类型: {}", dbType);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(dbType);
        paginationInterceptor.setMaxLimit(5000L); // 只读服务可以允许更大的查询量
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }

    /**
     * 创建自定义拦截器配置
     *
     * @param builder 配置构建器
     * @return 拦截器
     */
    public static MybatisPlusInterceptor createCustomInterceptor(InterceptorBuilder builder) {
        log.info("创建自定义MyBatis Plus拦截器");
        return builder.build();
    }

    /**
     * 拦截器配置构建器
     */
    public static class InterceptorBuilder {
        private final MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        private DbType dbType = DbType.MYSQL;

        public InterceptorBuilder dbType(DbType dbType) {
            this.dbType = dbType;
            return this;
        }

        public InterceptorBuilder pagination(boolean enable) {
            if (enable) {
                interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
            }
            return this;
        }

        public InterceptorBuilder pagination(boolean enable, Long maxLimit) {
            if (enable) {
                PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(dbType);
                if (maxLimit != null) {
                    paginationInterceptor.setMaxLimit(maxLimit);
                }
                interceptor.addInnerInterceptor(paginationInterceptor);
            }
            return this;
        }

        public InterceptorBuilder optimisticLocker(boolean enable) {
            if (enable) {
                interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
            }
            return this;
        }

        public InterceptorBuilder blockAttack(boolean enable) {
            if (enable) {
                interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
            }
            return this;
        }

        public InterceptorBuilder tenant(TenantLineHandler tenantLineHandler) {
            if (tenantLineHandler != null) {
                TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
                tenantInterceptor.setTenantLineHandler(tenantLineHandler);
                interceptor.addInnerInterceptor(tenantInterceptor);
            }
            return this;
        }

        public InterceptorBuilder addCustomInterceptor(InnerInterceptor innerInterceptor) {
            if (innerInterceptor != null) {
                interceptor.addInnerInterceptor(innerInterceptor);
            }
            return this;
        }

        public MybatisPlusInterceptor build() {
            return interceptor;
        }
    }
}
