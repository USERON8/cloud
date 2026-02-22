package com.cloud.order.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;








public class OrderServiceException extends BusinessException {

    





    public OrderServiceException(int code, String message) {
        super(code, message);
    }

    




    public OrderServiceException(String message) {
        this(ResultCode.ORDER_CREATE_FAILED, message);
    }

    




    public OrderServiceException(ResultCode resultCode) {
        this(resultCode, null);
    }

    





    public OrderServiceException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }

    





    public OrderServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    

    public static class OrderNotFoundException extends OrderServiceException {
        public OrderNotFoundException(String message) {
            super(ResultCode.ORDER_NOT_FOUND, message);
        }

        public OrderNotFoundException(Long orderId) {
            super(ResultCode.ORDER_NOT_FOUND, String.format("璁㈠崟涓嶅瓨鍦紝璁㈠崟ID: %d", orderId));
        }
    }

    public static class OrderCreateFailedException extends OrderServiceException {
        public OrderCreateFailedException(String message) {
            super(ResultCode.ORDER_CREATE_FAILED, message);
        }
    }

    public static class OrderUpdateFailedException extends OrderServiceException {
        public OrderUpdateFailedException(String message) {
            super(ResultCode.ORDER_UPDATE_FAILED, message);
        }
    }

    public static class OrderDeleteFailedException extends OrderServiceException {
        public OrderDeleteFailedException(String message) {
            super(ResultCode.ORDER_DELETE_FAILED, message);
        }
    }

    public static class OrderStatusErrorException extends OrderServiceException {
        public OrderStatusErrorException(String message) {
            super(ResultCode.ORDER_STATUS_ERROR, message);
        }
    }

    public static class OrderQueryFailedException extends OrderServiceException {
        public OrderQueryFailedException(String message) {
            super(ResultCode.ORDER_QUERY_FAILED, message);
        }
    }

    public static class OrderPaymentFailedException extends OrderServiceException {
        public OrderPaymentFailedException(String message) {
            super(ResultCode.BUSINESS_ERROR, "璁㈠崟鏀粯澶辫触: " + message);
        }
    }

    public static class OrderShippingFailedException extends OrderServiceException {
        public OrderShippingFailedException(String message) {
            super(ResultCode.BUSINESS_ERROR, "璁㈠崟鍙戣揣澶辫触: " + message);
        }
    }
}
