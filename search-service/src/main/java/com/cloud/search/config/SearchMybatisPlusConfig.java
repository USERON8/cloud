package com.cloud.search.config;

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
 * 搜索服务 MyBatis Plus 配置
 * 针对搜索业务场景进行优化配置，主要用于只读查询
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class SearchMybatisPlusConfig {

    /**
     * 搜索服务专用的MyBatis Plus拦截器配置
     * 使用只读服务配置，主要用于查询操作
     */
    @Bean
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("初始化搜索服务MyBatis Plus拦截器配置");
        return MybatisPlusConfigFactory.createReadOnlyInterceptor(DbType.MYSQL);
    }

    /**
     * 搜索服务专用的元数据处理器
     * 主要用于搜索日志记录等场景
     */
    @Bean
    @Primary
    public MetaObjectHandler metaObjectHandler() {
        log.info("初始化搜索服务元数据处理器");
        return new SearchMetaObjectHandler();
    }

    /**
     * 搜索服务专用的元数据处理器实现
     */
    public static class SearchMetaObjectHandler implements MetaObjectHandler {

        /**
         * 插入时自动填充
         * 主要用于搜索日志、统计数据等记录
         */
        @Override
        public void insertFill(MetaObject metaObject) {
            log.debug("搜索服务 - 开始插入填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());

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

            // 搜索特有字段填充
            fillSearchSpecificFields(metaObject);

            log.debug("搜索服务 - 插入填充完成");
        }

        /**
         * 更新时自动填充
         * 搜索服务很少有更新操作，主要用于统计数据更新
         */
        @Override
        public void updateFill(MetaObject metaObject) {
            log.debug("搜索服务 - 开始更新填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());

            LocalDateTime now = LocalDateTime.now();
            Long currentUserId = getCurrentUserId();

            // 基础字段填充
            this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);

            // 操作人字段填充
            if (metaObject.hasGetter("updateBy")) {
                this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
            }

            log.debug("搜索服务 - 更新填充完成");
        }

        /**
         * 填充搜索特有字段
         */
        private void fillSearchSpecificFields(MetaObject metaObject) {
            // 搜索状态默认值
            if (metaObject.hasGetter("searchStatus") && metaObject.getValue("searchStatus") == null) {
                this.strictInsertFill(metaObject, "searchStatus", Integer.class, 1); // 1-成功
            }

            // 搜索来源默认值
            if (metaObject.hasGetter("searchSource") && metaObject.getValue("searchSource") == null) {
                this.strictInsertFill(metaObject, "searchSource", String.class, "WEB");
            }

            // 搜索结果数量默认值
            if (metaObject.hasGetter("resultCount") && metaObject.getValue("resultCount") == null) {
                this.strictInsertFill(metaObject, "resultCount", Integer.class, 0);
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
