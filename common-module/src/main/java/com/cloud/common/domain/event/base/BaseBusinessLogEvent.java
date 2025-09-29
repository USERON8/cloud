package com.cloud.common.domain.event.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一业务日志事件基类
 * 
 * 定义所有业务日志事件的通用字段和结构，
 * 简化日志事件的创建和处理，确保日志格式统一
 * 
 * @author CloudDevAgent  
 * @since 2025-09-27
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseBusinessLogEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志唯一ID
     */
    private String logId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 业务模块
     */
    private String module;

    /**
     * 操作类型
     * CREATE - 创建
     * UPDATE - 更新  
     * DELETE - 删除
     * STATUS_CHANGE - 状态变更
     * COMPLETE - 完成
     * CANCEL - 取消
     * REFUND - 退款
     */
    private String operation;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 业务ID（如订单ID、商品ID、用户ID等）
     */
    private String businessId;

    /**
     * 业务类型
     * USER - 用户
     * PRODUCT - 商品
     * SHOP - 店铺
     * ORDER - 订单
     * PAYMENT - 支付
     */
    private String businessType;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名称
     */
    private String userName;

    /**
     * 用户类型
     * CUSTOMER - 客户
     * MERCHANT - 商家
     * ADMIN - 管理员
     * SYSTEM - 系统
     */
    private String userType;

    /**
     * 操作结果
     * SUCCESS - 成功
     * FAILURE - 失败
     */
    private String result;

    /**
     * 数据变更前的值（JSON格式）
     */
    private String beforeData;

    /**
     * 数据变更后的值（JSON格式）
     */
    private String afterData;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 分布式追踪ID
     */
    private String traceId;

    /**
     * 操作人（可能是用户名或系统标识）
     */
    private String operator;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 获取日志类型，子类需要实现此方法
     * 
     * @return 日志类型标识
     */
    public abstract String getLogType();
}
