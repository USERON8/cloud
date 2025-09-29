package com.cloud.log.service.impl;

import com.cloud.common.domain.event.base.BaseBusinessLogEvent;
import com.cloud.common.domain.event.order.OrderOperationLogEvent;
import com.cloud.common.domain.event.payment.PaymentOperationLogEvent;
import com.cloud.common.domain.event.product.ProductChangeLogEvent;
import com.cloud.common.domain.event.product.ShopChangeLogEvent;
import com.cloud.common.domain.event.user.UserChangeLogEvent;
import com.cloud.log.service.UnifiedBusinessLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 统一业务日志服务实现
 * 
 * 基于Elasticsearch存储各种类型的业务日志，
 * 使用Redis进行幂等性检查和缓存管理
 * 
 * @author CloudDevAgent
 * @since 2025-09-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedBusinessLogServiceImpl implements UnifiedBusinessLogService {

    private final ElasticsearchClient elasticsearchClient;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String PROCESSED_LOG_PREFIX = "processed_log:";
    private static final String USER_LOG_INDEX = "business-log-user";
    private static final String PRODUCT_LOG_INDEX = "business-log-product";
    private static final String SHOP_LOG_INDEX = "business-log-shop";
    private static final String ORDER_LOG_INDEX = "business-log-order";
    private static final String PAYMENT_LOG_INDEX = "business-log-payment";
    private static final String GENERIC_LOG_INDEX = "business-log-generic";

    @Override
    public boolean isLogProcessed(String logId) {
        try {
            String key = PROCESSED_LOG_PREFIX + logId;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("检查日志处理状态失败 - 日志ID: {}, 错误: {}", logId, e.getMessage());
            return false; // 发生异常时假设未处理，允许重新处理
        }
    }

    @Override
    public void markLogProcessed(String logId) {
        try {
            String key = PROCESSED_LOG_PREFIX + logId;
            // 设置过期时间为7天，避免Redis内存无限增长
            redisTemplate.opsForValue().set(key, "1", 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("标记日志已处理失败 - 日志ID: {}, 错误: {}", logId, e.getMessage());
        }
    }

    @Override
    public boolean saveUserChangeLog(UserChangeLogEvent event) {
        return saveToElasticsearch(USER_LOG_INDEX, event, buildUserChangeLogDocument(event));
    }

    @Override
    public boolean saveProductChangeLog(ProductChangeLogEvent event) {
        return saveToElasticsearch(PRODUCT_LOG_INDEX, event, buildProductChangeLogDocument(event));
    }

    @Override
    public boolean saveShopChangeLog(ShopChangeLogEvent event) {
        return saveToElasticsearch(SHOP_LOG_INDEX, event, buildShopChangeLogDocument(event));
    }

    @Override
    public boolean saveOrderOperationLog(OrderOperationLogEvent event) {
        return saveToElasticsearch(ORDER_LOG_INDEX, event, buildOrderOperationLogDocument(event));
    }

    @Override
    public boolean savePaymentOperationLog(PaymentOperationLogEvent event) {
        return saveToElasticsearch(PAYMENT_LOG_INDEX, event, buildPaymentOperationLogDocument(event));
    }

    @Override
    public boolean saveGenericBusinessLog(BaseBusinessLogEvent event) {
        return saveToElasticsearch(GENERIC_LOG_INDEX, event, buildGenericLogDocument(event));
    }

    /**
     * 保存到Elasticsearch
     */
    private boolean saveToElasticsearch(String indexName, BaseBusinessLogEvent event, Map<String, Object> document) {
        try {
            // 使用日期后缀的索引名，便于按日期管理日志
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String fullIndexName = indexName + "-" + dateStr;

            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                    .index(fullIndexName)
                    .id(event.getLogId())
                    .document(document)
            );

            IndexResponse response = elasticsearchClient.index(request);

            boolean success = response.result().name().equals("CREATED") ||
                             response.result().name().equals("UPDATED");

            if (success) {
                log.debug("业务日志保存到ES成功 - 索引: {}, 日志ID: {}, 类型: {}",
                         fullIndexName, event.getLogId(), event.getLogType());
            } else {
                log.warn("业务日志保存到ES失败 - 索引: {}, 日志ID: {}, 响应: {}",
                        fullIndexName, event.getLogId(), response.result());
            }

            return success;
        } catch (Exception e) {
            log.error("保存业务日志到ES异常 - 索引: {}, 日志ID: {}, 错误: {}", 
                     indexName, event.getLogId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建用户变更日志文档
     */
    private Map<String, Object> buildUserChangeLogDocument(UserChangeLogEvent event) {
        Map<String, Object> document = buildBaseLogDocument(event);
        document.put("email", event.getEmail());
        document.put("phone", event.getPhone());
        document.put("statusChange", event.getStatusChange());
        document.put("ipAddress", event.getIpAddress());
        document.put("deviceInfo", event.getDeviceInfo());
        document.put("roleChange", event.getRoleChange());
        return document;
    }

    /**
     * 构建商品变更日志文档
     */
    private Map<String, Object> buildProductChangeLogDocument(ProductChangeLogEvent event) {
        Map<String, Object> document = buildBaseLogDocument(event);
        document.put("productName", event.getProductName());
        document.put("productSku", event.getProductSku());
        document.put("categoryId", event.getCategoryId());
        document.put("categoryName", event.getCategoryName());
        document.put("shopId", event.getShopId());
        document.put("shopName", event.getShopName());
        document.put("priceChange", event.getPriceChange());
        document.put("stockChange", event.getStockChange());
        document.put("statusChange", event.getStatusChange());
        return document;
    }

    /**
     * 构建店铺变更日志文档
     */
    private Map<String, Object> buildShopChangeLogDocument(ShopChangeLogEvent event) {
        Map<String, Object> document = buildBaseLogDocument(event);
        document.put("shopName", event.getShopName());
        document.put("shopCode", event.getShopCode());
        document.put("ownerId", event.getOwnerId());
        document.put("ownerName", event.getOwnerName());
        document.put("shopType", event.getShopType());
        document.put("statusChange", event.getStatusChange());
        document.put("verificationChange", event.getVerificationChange());
        document.put("categoryChange", event.getCategoryChange());
        document.put("contactPhone", event.getContactPhone());
        document.put("shopAddress", event.getShopAddress());
        return document;
    }

    /**
     * 构建订单操作日志文档
     */
    private Map<String, Object> buildOrderOperationLogDocument(OrderOperationLogEvent event) {
        Map<String, Object> document = buildBaseLogDocument(event);
        document.put("orderNo", event.getOrderNo());
        document.put("orderAmount", event.getOrderAmount());
        document.put("paymentAmount", event.getPaymentAmount());
        document.put("refundAmount", event.getRefundAmount());
        document.put("statusChange", event.getStatusChange());
        document.put("refundReason", event.getRefundReason());
        document.put("refundType", event.getRefundType());
        document.put("totalQuantity", event.getTotalQuantity());
        document.put("shopId", event.getShopId());
        document.put("shopName", event.getShopName());
        document.put("paymentMethod", event.getPaymentMethod());
        document.put("completionTime", event.getCompletionTime());
        return document;
    }

    /**
     * 构建支付操作日志文档
     */
    private Map<String, Object> buildPaymentOperationLogDocument(PaymentOperationLogEvent event) {
        Map<String, Object> document = buildBaseLogDocument(event);
        document.put("paymentId", event.getPaymentId());
        document.put("orderId", event.getOrderId());
        document.put("orderNo", event.getOrderNo());
        document.put("paymentAmount", event.getPaymentAmount());
        document.put("refundAmount", event.getRefundAmount());
        document.put("paymentMethod", event.getPaymentMethod());
        document.put("thirdPartyTransactionId", event.getThirdPartyTransactionId());
        document.put("statusChange", event.getStatusChange());
        document.put("refundReason", event.getRefundReason());
        document.put("refundType", event.getRefundType());
        document.put("refundId", event.getRefundId());
        document.put("completionTime", event.getCompletionTime());
        document.put("channelReturnCode", event.getChannelReturnCode());
        document.put("channelReturnMessage", event.getChannelReturnMessage());
        return document;
    }

    /**
     * 构建通用日志文档
     */
    private Map<String, Object> buildGenericLogDocument(BaseBusinessLogEvent event) {
        return buildBaseLogDocument(event);
    }

    /**
     * 构建基础日志文档
     */
    private Map<String, Object> buildBaseLogDocument(BaseBusinessLogEvent event) {
        Map<String, Object> document = new HashMap<>();
        document.put("logId", event.getLogId());
        document.put("serviceName", event.getServiceName());
        document.put("module", event.getModule());
        document.put("operation", event.getOperation());
        document.put("description", event.getDescription());
        document.put("businessId", event.getBusinessId());
        document.put("businessType", event.getBusinessType());
        document.put("userId", event.getUserId());
        document.put("userName", event.getUserName());
        document.put("userType", event.getUserType());
        document.put("result", event.getResult());
        document.put("beforeData", event.getBeforeData());
        document.put("afterData", event.getAfterData());
        document.put("operationTime", event.getOperationTime());
        document.put("traceId", event.getTraceId());
        document.put("operator", event.getOperator());
        document.put("remark", event.getRemark());
        document.put("logType", event.getLogType());
        document.put("indexTime", LocalDateTime.now());
        return document;
    }
}
