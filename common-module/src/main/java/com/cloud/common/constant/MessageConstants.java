package com.cloud.common.constant;

/**
 * 消息常量类
 * 定义消息相关的常量
 *
 * @author cloud
 */
public final class MessageConstants {

    private MessageConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 消息绑定名称
     */
    public static final class Bindings {
        public static final String USER_EVENTS = "userEvents-out-0";
        public static final String ORDER_EVENTS = "orderEvents-out-0";
        public static final String PAYMENT_EVENTS = "paymentEvents-out-0";
        public static final String LOG_EVENTS = "logEvents-out-0";
        public static final String NOTIFICATION_EVENTS = "notificationEvents-out-0";
    }

    /**
     * 消息主题
     */
    public static final class Topics {
        public static final String USER_TOPIC = "user-events-topic";
        public static final String ORDER_TOPIC = "order-events-topic";
        public static final String PAYMENT_TOPIC = "payment-events-topic";
        public static final String LOG_TOPIC = "log-events-topic";
        public static final String NOTIFICATION_TOPIC = "notification-events-topic";
    }

    /**
     * 消费者组
     */
    public static final class ConsumerGroups {
        public static final String USER_SERVICE = "user-service-consumer";
        public static final String ORDER_SERVICE = "order-service-consumer";
        public static final String PAYMENT_SERVICE = "payment-service-consumer";
        public static final String LOG_SERVICE = "log-service-consumer";
        public static final String NOTIFICATION_SERVICE = "notification-service-consumer";
    }

    /**
     * 消息标签
     */
    public static final class Tags {
        public static final String USER_CREATED = "USER_CREATED";
        public static final String USER_UPDATED = "USER_UPDATED";
        public static final String USER_DELETED = "USER_DELETED";

        public static final String ORDER_CREATED = "ORDER_CREATED";
        public static final String ORDER_PAID = "ORDER_PAID";
        public static final String ORDER_COMPLETED = "ORDER_COMPLETED";
        public static final String ORDER_CANCELLED = "ORDER_CANCELLED";

        public static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
        public static final String PAYMENT_FAILED = "PAYMENT_FAILED";

        public static final String LOG_COLLECTION = "LOG_COLLECTION";
        public static final String NOTIFICATION_SEND = "NOTIFICATION_SEND";
    }

    /**
     * 消息优先级
     */
    public static final class Priority {
        public static final String HIGH = "HIGH";
        public static final String NORMAL = "NORMAL";
        public static final String LOW = "LOW";
    }
}
