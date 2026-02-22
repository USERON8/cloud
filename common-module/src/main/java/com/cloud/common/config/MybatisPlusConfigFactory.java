package com.cloud.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import lombok.extern.slf4j.Slf4j;









@Slf4j
public class MybatisPlusConfigFactory {

    






    public static MybatisPlusInterceptor createBasicInterceptor(DbType dbType) {
        
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));

        return interceptor;
    }

    






    public static MybatisPlusInterceptor createStandardInterceptor(DbType dbType) {
        
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));

        
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    







    public static MybatisPlusInterceptor createHighConcurrencyInterceptor(DbType dbType) {
        
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(dbType);
        paginationInterceptor.setMaxLimit(1000L); 
        interceptor.addInnerInterceptor(paginationInterceptor);

        
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    








    public static MybatisPlusInterceptor createMultiTenantInterceptor(DbType dbType,
                                                                      TenantLineHandler tenantLineHandler) {
        
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(tenantLineHandler);
        interceptor.addInnerInterceptor(tenantInterceptor);

        
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));

        
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    







    public static MybatisPlusInterceptor createReadOnlyInterceptor(DbType dbType) {
        
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(dbType);
        paginationInterceptor.setMaxLimit(5000L); 
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }

    





    public static MybatisPlusInterceptor createCustomInterceptor(InterceptorBuilder builder) {
        return builder.build();
    }

    


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
