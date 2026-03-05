package com.cloud.common.exception;

public class EntityNotFoundException extends ResourceNotFoundException {

    private static final String DEFAULT_MESSAGE = "%s not found";

    public EntityNotFoundException(String entityName, Object id) {
        super(String.format("%s not found, id: %s", entityName, id));
    }

    public EntityNotFoundException(String entityName, String condition) {
        super(String.format("%s not found, %s", entityName, condition));
    }

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    public static EntityNotFoundException order(Long orderId) {
        return new EntityNotFoundException("Order", orderId);
    }

    public static EntityNotFoundException user(Long userId) {
        return new EntityNotFoundException("User", userId);
    }

    public static EntityNotFoundException product(Long productId) {
        return new EntityNotFoundException("Product", productId);
    }

    public static EntityNotFoundException stock(Long productId) {
        return new EntityNotFoundException("Stock", "productId: " + productId);
    }

    public static EntityNotFoundException payment(Object paymentInfo) {
        return new EntityNotFoundException("Payment", paymentInfo);
    }

    public static EntityNotFoundException merchant(Long merchantId) {
        return new EntityNotFoundException("Merchant", merchantId);
    }
}
