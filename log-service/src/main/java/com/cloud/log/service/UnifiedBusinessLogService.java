package com.cloud.log.service;

import com.cloud.common.domain.event.base.BaseBusinessLogEvent;
import com.cloud.common.domain.event.order.OrderOperationLogEvent;
import com.cloud.common.domain.event.payment.PaymentOperationLogEvent;
import com.cloud.common.domain.event.product.ProductChangeLogEvent;
import com.cloud.common.domain.event.product.ShopChangeLogEvent;
import com.cloud.common.domain.event.user.UserChangeLogEvent;

/**
 * 统一业务日志服务接口
 * 
 * 提供各种类型业务日志的统一处理和存储服务
 * 
 * @author CloudDevAgent
 * @since 2025-09-27
 */
public interface UnifiedBusinessLogService {

    /**
     * 检查日志是否已处理（幂等性检查）
     * 
     * @param logId 日志ID
     * @return 是否已处理
     */
    boolean isLogProcessed(String logId);

    /**
     * 标记日志已处理
     * 
     * @param logId 日志ID
     */
    void markLogProcessed(String logId);

    /**
     * 保存用户变更日志
     * 
     * @param event 用户变更日志事件
     * @return 保存是否成功
     */
    boolean saveUserChangeLog(UserChangeLogEvent event);

    /**
     * 保存商品变更日志
     * 
     * @param event 商品变更日志事件
     * @return 保存是否成功
     */
    boolean saveProductChangeLog(ProductChangeLogEvent event);

    /**
     * 保存店铺变更日志
     * 
     * @param event 店铺变更日志事件
     * @return 保存是否成功
     */
    boolean saveShopChangeLog(ShopChangeLogEvent event);

    /**
     * 保存订单操作日志
     * 
     * @param event 订单操作日志事件
     * @return 保存是否成功
     */
    boolean saveOrderOperationLog(OrderOperationLogEvent event);

    /**
     * 保存支付操作日志
     * 
     * @param event 支付操作日志事件
     * @return 保存是否成功
     */
    boolean savePaymentOperationLog(PaymentOperationLogEvent event);

    /**
     * 保存通用业务日志
     * 
     * @param event 基础业务日志事件
     * @return 保存是否成功
     */
    boolean saveGenericBusinessLog(BaseBusinessLogEvent event);
}
