package com.cloud.order.exception;








public class OrderBusinessException extends RuntimeException {

    


    private final Long orderId;

    


    private final String errorCode;

    public OrderBusinessException(String message) {
        super(message);
        this.orderId = null;
        this.errorCode = null;
    }

    public OrderBusinessException(String message, Throwable cause) {
        super(message, cause);
        this.orderId = null;
        this.errorCode = null;
    }

    public OrderBusinessException(Long orderId, String message) {
        super(message);
        this.orderId = orderId;
        this.errorCode = null;
    }

    public OrderBusinessException(Long orderId, String errorCode, String message) {
        super(message);
        this.orderId = orderId;
        this.errorCode = errorCode;
    }

    public OrderBusinessException(Long orderId, String message, Throwable cause) {
        super(message, cause);
        this.orderId = orderId;
        this.errorCode = null;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getCode() {
        
        if (errorCode != null) {
            try {
                return Integer.parseInt(errorCode);
            } catch (NumberFormatException e) {
                return 500;
            }
        }
        return 500;
    }
}
