package com.cloud.common.constant;

/**
 * 消息主题常量定义
 * 统一管理所有RocketMQ Topic名称和相关配置
 *
 * @author cloud
 * @date 2025/1/15
 */
public final class MessageTopicConstants {

    /**
     * 日志收集主题
     * 用途: 除auth-service和gateway-service外的所有微服务发送日志到log-service
     * 生产者: user-service, product-service, stock-service, payment-service, order-service, search-service
     * 消费者: log-service
     */
    public static final String LOG_COLLECTION_TOPIC = "log-collection-topic";

    // ================================ 日志收集Topic ================================
    /**
     * 订单创建主题
     * 用途: 订单创建时通知支付服务创建支付记录，通知库存服务冻结库存
     * 生产者: order-service
     * 消费者: payment-service, stock-service
     */
    public static final String ORDER_CREATED_TOPIC = "order-created-topic";

    // ================================ 订单业务流程Topic ================================
    /**
     * 支付成功主题
     * 用途: 支付成功后通知订单服务更新订单状态
     * 生产者: payment-service
     * 消费者: order-service
     */
    public static final String PAYMENT_SUCCESS_TOPIC = "payment-success-topic";
    /**
     * 订单完成主题
     * 用途: 订单完成后通知库存服务解冻并扣减库存
     * 生产者: order-service
     * 消费者: stock-service
     */
    public static final String ORDER_COMPLETED_TOPIC = "order-completed-topic";

    private MessageTopicConstants() {
        // 工具类，禁止实例化
    }

    // ================================ 消息标签常量 ================================

    /**
     * 日志收集相关标签
     */
    public static final class LogTags {
        public static final String USER_LOG = "user-log";
        public static final String PRODUCT_LOG = "product-log";
        public static final String STOCK_LOG = "stock-log";
        public static final String PAYMENT_LOG = "payment-log";
        public static final String ORDER_LOG = "order-log";
        public static final String SEARCH_LOG = "search-log";
    }

    /**
     * 订单业务相关标签
     */
    public static final class OrderTags {
        public static final String ORDER_CREATED = "order-created";
        public static final String ORDER_PAID = "order-paid";
        public static final String ORDER_SHIPPED = "order-shipped";
        public static final String ORDER_COMPLETED = "order-completed";
        public static final String ORDER_CANCELLED = "order-cancelled";
        public static final String ORDER_REFUNDED = "order-refunded";
    }

    /**
     * 支付业务相关标签
     */
    public static final class PaymentTags {
        public static final String PAYMENT_CREATED = "payment-created";
        public static final String PAYMENT_PROCESSING = "payment-processing";
        public static final String PAYMENT_SUCCESS = "payment-success";
        public static final String PAYMENT_FAILED = "payment-failed";
        public static final String PAYMENT_TIMEOUT = "payment-timeout";
        public static final String REFUND_APPLIED = "refund-applied";
        public static final String REFUND_SUCCESS = "refund-success";
        public static final String REFUND_FAILED = "refund-failed";
    }

    /**
     * 库存业务相关标签
     */
    public static final class StockTags {
        public static final String STOCK_FROZEN = "stock-frozen";
        public static final String STOCK_UNFROZEN = "stock-unfrozen";
        public static final String STOCK_DEDUCTED = "stock-deducted";
        public static final String STOCK_RESTORED = "stock-restored";
        public static final String STOCK_RESERVED = "stock-reserved";
        public static final String STOCK_RELEASED = "stock-released";
    }

    // ================================ 消费者组常量 ================================

    /**
     * 日志服务消费者组
     */
    public static final class LogConsumerGroups {
        public static final String LOG_COLLECTION_GROUP = "log-collection-consumer-group";
    }

    /**
     * 订单服务消费者组
     */
    public static final class OrderConsumerGroups {
        public static final String PAYMENT_SUCCESS_GROUP = "order-payment-success-group";
    }

    /**
     * 支付服务消费者组
     */
    public static final class PaymentConsumerGroups {
        public static final String ORDER_CREATED_GROUP = "payment-order-created-group";
    }

    /**
     * 库存服务消费者组
     */
    public static final class StockConsumerGroups {
        public static final String ORDER_CREATED_GROUP = "stock-order-created-group";
        public static final String ORDER_COMPLETED_GROUP = "stock-order-completed-group";
    }

    // ================================ 绑定名称常量 ================================

    /**
     * 生产者绑定名称
     */
    public static final class ProducerBindings {
        // 日志收集生产者
        public static final String LOG_PRODUCER = "logProducer-out-0";

        // 订单业务生产者
        public static final String ORDER_CREATED_PRODUCER = "orderCreatedProducer-out-0";
        public static final String ORDER_COMPLETED_PRODUCER = "orderCompletedProducer-out-0";

        // 支付业务生产者
        public static final String PAYMENT_SUCCESS_PRODUCER = "paymentSuccessProducer-out-0";
    }

    /**
     * 消费者绑定名称
     */
    public static final class ConsumerBindings {
        // 日志收集消费者
        public static final String LOG_CONSUMER = "logConsumer-in-0";

        // 订单业务消费者
        public static final String ORDER_CREATED_CONSUMER = "orderCreatedConsumer-in-0";
        public static final String ORDER_COMPLETED_CONSUMER = "orderCompletedConsumer-in-0";
        public static final String PAYMENT_SUCCESS_CONSUMER = "paymentSuccessConsumer-in-0";
    }

    // ================================ 配置常量 ================================

    /**
     * 默认配置值
     */
    public static final class DefaultConfig {
        public static final int DEFAULT_PARTITION_COUNT = 4;
        public static final int DEFAULT_CONSUMER_THREAD_MIN = 5;
        public static final int DEFAULT_CONSUMER_THREAD_MAX = 20;
        public static final int DEFAULT_PULL_BATCH_SIZE = 16;
        public static final int DEFAULT_CONSUME_TIMEOUT = 10000;
        public static final int DEFAULT_MAX_ATTEMPTS = 3;
        public static final int DEFAULT_BACK_OFF_INITIAL_INTERVAL = 1000;
        public static final int DEFAULT_BACK_OFF_MAX_INTERVAL = 10000;
        public static final double DEFAULT_BACK_OFF_MULTIPLIER = 2.0;
        public static final int DEFAULT_SEND_MESSAGE_TIMEOUT = 3000;
        public static final int DEFAULT_MAX_MESSAGE_SIZE = 4194304; // 4MB
        public static final int DEFAULT_COMPRESS_THRESHOLD = 4096; // 4KB
        public static final int DEFAULT_RETRY_TIMES = 2;
    }
}
