package com.cloud.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * 基础数据源配置类
 * 包含 MyBatis-Plus 相关配置：分页、乐观锁、逻辑删除等
 * 
 * @author what's up
 */
@Slf4j
@Configuration
@ConditionalOnClass({MybatisPlusInterceptor.class})
public class BaseDataSourceConfig implements MetaObjectHandler {

    @Value("${spring.application.name:cloud-service}")
    private String applicationName;

    /**
     * MyBatis-Plus 拦截器配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件（必须指定数据库类型）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        //乐观锁插件（按需）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        //防全表更新插件（按需）
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    /**
     * SQL 注入器
     */
    @Bean
    public ISqlInjector sqlInjector() {
        return new DefaultSqlInjector();
    }
    
    /**
     * 动态Mapper扫描配置
     * 根据服务名自动扫描对应的mapper包
     */
    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        // 动态构造扫描包路径
        String basePackage = getMapperScanPackage();
        log.info("设置Mapper扫描包路径: {}", basePackage);
        configurer.setBasePackage(basePackage);
        return configurer;
    }
    
    /**
     * 获取Mapper扫描包路径
     * 子类可以重写此方法自定义扫描路径
     */
    protected String getMapperScanPackage() {
        // 基于服务名构造默认的mapper包路径
        if (applicationName.endsWith("-service")) {
            String serviceName = applicationName.replace("-service", "");
            return "com.cloud." + serviceName + ".mapper";
        }
        return "com.cloud." + applicationName + ".mapper";
    }

    /**
     * 全局配置
     * 设置逻辑删除配置
     */
    @Bean
    public GlobalConfig globalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        
        // 设置逻辑删除配置
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setLogicDeleteField("deleted");  // 逻辑删除字段名
        dbConfig.setLogicDeleteValue("1");        // 逻辑已删除值(默认为1)
        dbConfig.setLogicNotDeleteValue("0");     // 逻辑未删除值(默认为0)
        
        globalConfig.setDbConfig(dbConfig);
        globalConfig.setMetaObjectHandler(this);
        
        return globalConfig;
    }

    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");
        // 填充创建时间
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        // 填充更新时间
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");
        // 填充更新时间
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
