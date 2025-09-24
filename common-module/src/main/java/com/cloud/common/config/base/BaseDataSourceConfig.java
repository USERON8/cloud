package com.cloud.common.config.base;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * 基础数据源配置抽象类
 * 提供MyBatis Plus基本配置模板，各服务按需继承和扩展
 * <p>
 * 遵循服务自治原则：
 * - common-module只提供基础模板和通用的MetaObjectHandler
 * - 具体服务自行决定是否启用数据库和如何配置MyBatis Plus
 *
 * @author what's up
 */
@Slf4j
public abstract class BaseDataSourceConfig implements MetaObjectHandler {

    /**
     * 创建标准的MyBatis Plus拦截器配置
     * 子类可以重写此方法来自定义拦截器
     *
     * @return 配置好的拦截器
     */
    protected MybatisPlusInterceptor createMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件（指定数据库类型，子类可重写）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(getDbType()));

        // 乐观锁插件（子类可选择是否启用）
        if (shouldEnableOptimisticLocker()) {
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        }

        // 防全表更新插件（子类可选择是否启用）
        if (shouldEnableBlockAttack()) {
            interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        }

        return interceptor;
    }

    /**
     * 获取数据库类型，子类可以重写
     * 默认为MySQL
     *
     * @return 数据库类型
     */
    protected DbType getDbType() {
        return DbType.MYSQL;
    }

    /**
     * 是否启用乐观锁插件
     * 子类可以重写此方法
     *
     * @return 是否启用乐观锁
     */
    protected boolean shouldEnableOptimisticLocker() {
        return true;
    }

    /**
     * 是否启用防全表更新插件
     * 子类可以重写此方法
     *
     * @return 是否启用防全表更新
     */
    protected boolean shouldEnableBlockAttack() {
        return true;
    }

    /**
     * 插入时自动填充
     * 提供通用的字段填充逻辑，子类可以重写扩展
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");

        // 填充创建时间和更新时间
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());

        // 填充版本号（乐观锁）
        this.strictInsertFill(metaObject, "version", Integer.class, 1);

        // 子类可以扩展更多填充逻辑
        doInsertFill(metaObject);
    }

    /**
     * 更新时自动填充
     * 提供通用的字段填充逻辑，子类可以重写扩展
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");

        // 填充更新时间
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());

        // 子类可以扩展更多填充逻辑
        doUpdateFill(metaObject);
    }

    /**
     * 子类可以重写此方法来扩展插入时的填充逻辑
     *
     * @param metaObject 元对象
     */
    protected void doInsertFill(MetaObject metaObject) {
        // 默认空实现，子类可以重写
    }

    /**
     * 子类可以重写此方法来扩展更新时的填充逻辑
     *
     * @param metaObject 元对象
     */
    protected void doUpdateFill(MetaObject metaObject) {
        // 默认空实现，子类可以重写
    }

    /**
     * 获取当前用户ID
     * 子类可以重写此方法来获取当前登录用户ID
     *
     * @return 当前用户ID
     */
    protected Long getCurrentUserId() {
        // 默认返回系统用户ID，子类可以重写获取真实用户ID
        return 0L;
    }

    /**
     * 获取当前操作人信息
     * 子类可以重写此方法来获取当前操作人信息
     *
     * @return 操作人信息
     */
    protected String getCurrentOperator() {
        // 默认返回系统，子类可以重写获取真实操作人
        return "SYSTEM";
    }
}
