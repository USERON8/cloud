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














@Slf4j
@Configuration
public class MybatisPlusConfig {

    









    @Bean
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(1000L); 
        paginationInterceptor.setOverflow(false); 
        interceptor.addInnerInterceptor(paginationInterceptor);

        
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    










    @Bean
    @Primary
    public MetaObjectHandler metaObjectHandler() {
        
        return new UnifiedMetaObjectHandler();
    }

    








    public static class UnifiedMetaObjectHandler implements MetaObjectHandler {

        






        @Override
        public void insertFill(MetaObject metaObject) {
            log.debug("寮€濮嬫彃鍏ュ～鍏?- 瀹炰綋绫? {}", metaObject.getOriginalObject().getClass().getSimpleName());

            LocalDateTime now = LocalDateTime.now();
            Long currentUserId = getCurrentUserId();

            
            fillDateTimeField(metaObject, new String[]{"createdAt", "created_at", "createTime", "create_time"}, now);

            
            fillDateTimeField(metaObject, new String[]{"updatedAt", "updated_at", "updateTime", "update_time"}, now);

            
            fillField(metaObject, new String[]{"version"}, Integer.class, 1);

            
            fillField(metaObject, new String[]{"createBy", "create_by", "createdBy", "created_by"}, Long.class, currentUserId);

            
            fillField(metaObject, new String[]{"updateBy", "update_by", "updatedBy", "updated_by"}, Long.class, currentUserId);

            
            fillField(metaObject, new String[]{"deleted", "is_deleted"}, Integer.class, 0);

            log.debug("鎻掑叆濉厖瀹屾垚 - createdAt: {}, updatedAt: {}, userId: {}", now, now, currentUserId);
        }

        




        @Override
        public void updateFill(MetaObject metaObject) {
            log.debug("寮€濮嬫洿鏂板～鍏?- 瀹炰綋绫? {}", metaObject.getOriginalObject().getClass().getSimpleName());

            LocalDateTime now = LocalDateTime.now();
            Long currentUserId = getCurrentUserId();

            
            updateDateTimeField(metaObject, new String[]{"updatedAt", "updated_at", "updateTime", "update_time"}, now);

            
            updateField(metaObject, new String[]{"updateBy", "update_by", "updatedBy", "updated_by"}, Long.class, currentUserId);

            log.debug("鏇存柊濉厖瀹屾垚 - updatedAt: {}, userId: {}", now, currentUserId);
        }

        


        private <T> void fillField(MetaObject metaObject, String[] fieldNames, Class<T> fieldType, T value) {
            for (String fieldName : fieldNames) {
                if (metaObject.hasGetter(fieldName)) {
                    this.strictInsertFill(metaObject, fieldName, fieldType, value);
                    log.debug("濉厖瀛楁: {} = {}", fieldName, value);
                    break; 
                }
            }
        }

        


        private void fillDateTimeField(MetaObject metaObject, String[] fieldNames, LocalDateTime value) {
            fillField(metaObject, fieldNames, LocalDateTime.class, value);
        }

        


        private <T> void updateField(MetaObject metaObject, String[] fieldNames, Class<T> fieldType, T value) {
            for (String fieldName : fieldNames) {
                if (metaObject.hasGetter(fieldName)) {
                    this.strictUpdateFill(metaObject, fieldName, fieldType, value);
                    log.debug("鏇存柊瀛楁: {} = {}", fieldName, value);
                    break; 
                }
            }
        }

        


        private void updateDateTimeField(MetaObject metaObject, String[] fieldNames, LocalDateTime value) {
            updateField(metaObject, fieldNames, LocalDateTime.class, value);
        }

        





        private Long getCurrentUserId() {
            try {
                Long userId = SecurityUtils.getCurrentUserId();
                return userId != null ? userId : 0L;
            } catch (Exception e) {
                log.debug("鑾峰彇褰撳墠鐢ㄦ埛ID澶辫触锛屼娇鐢ㄧ郴缁熺敤鎴稩D: {}", e.getMessage());
                return 0L; 
            }
        }
    }
}
