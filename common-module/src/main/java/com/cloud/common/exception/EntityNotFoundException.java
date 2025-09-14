package com.cloud.common.exception;

/**
 * 实体未找到异常 - 通用异常类
 * 用于替代各服务中的 *NotFoundException
 *
 * @author what's up
 */
public class EntityNotFoundException extends ResourceNotFoundException {

    private static final String DEFAULT_MESSAGE = "%s不存在";

    /**
     * 通过实体名称和ID构造异常
     */
    public EntityNotFoundException(String entityName, Object id) {
        super(String.format("%s不存在，ID: %s", entityName, id));
    }

    /**
     * 通过实体名称和多个条件构造异常
     */
    public EntityNotFoundException(String entityName, String condition) {
        super(String.format("%s不存在，%s", entityName, condition));
    }

    /**
     * 直接指定消息
     */
    public EntityNotFoundException(String message) {
        super(message);
    }

    /**
     * 带异常原因
     */
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, String.valueOf(cause));
    }

    // 静态工厂方法，用于常见场景
    public static EntityNotFoundException order(Long orderId) {
        return new EntityNotFoundException("订单", orderId);
    }

    public static EntityNotFoundException user(Long userId) {
        return new EntityNotFoundException("用户", userId);
    }

    public static EntityNotFoundException product(Long productId) {
        return new EntityNotFoundException("商品", productId);
    }

    public static EntityNotFoundException stock(Long productId) {
        return new EntityNotFoundException("库存信息", "商品ID: " + productId);
    }

    public static EntityNotFoundException payment(Object paymentInfo) {
        return new EntityNotFoundException("支付记录", paymentInfo);
    }

    public static EntityNotFoundException merchant(Long merchantId) {
        return new EntityNotFoundException("商家", merchantId);
    }
}
