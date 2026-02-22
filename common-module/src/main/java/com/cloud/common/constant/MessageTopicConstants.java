package com.cloud.common.constant;








public final class MessageTopicConstants {

    





    public static final String LOG_COLLECTION_TOPIC = "log-collection-topic";

    
    





    public static final String ORDER_CREATED_TOPIC = "order-created-topic";

    
    





    public static final String PAYMENT_SUCCESS_TOPIC = "payment-success-topic";
    





    public static final String ORDER_COMPLETED_TOPIC = "order-completed-topic";

    private MessageTopicConstants() {
        
    }

    

    


    public static final class LogTags {
        public static final String USER_LOG = "user-log";
        public static final String PRODUCT_LOG = "product-log";
        public static final String STOCK_LOG = "stock-log";
        public static final String PAYMENT_LOG = "payment-log";
        public static final String ORDER_LOG = "order-log";
        public static final String SEARCH_LOG = "search-log";
    }

    


    public static final class OrderTags {
        public static final String ORDER_CREATED = "order-created";
        public static final String ORDER_PAID = "order-paid";
        public static final String ORDER_SHIPPED = "order-shipped";
        public static final String ORDER_COMPLETED = "order-completed";
        public static final String ORDER_CANCELLED = "order-cancelled";
        public static final String ORDER_REFUNDED = "order-refunded";
    }

    


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

    


    public static final class StockTags {
        public static final String STOCK_FROZEN = "stock-frozen";
        public static final String STOCK_UNFROZEN = "stock-unfrozen";
        public static final String STOCK_DEDUCTED = "stock-deducted";
        public static final String STOCK_RESTORED = "stock-restored";
        public static final String STOCK_RESERVED = "stock-reserved";
        public static final String STOCK_RELEASED = "stock-released";
    }

    

    


    public static final class LogConsumerGroups {
        public static final String LOG_COLLECTION_GROUP = "log-collection-consumer-group";
    }

    


    public static final class OrderConsumerGroups {
        public static final String PAYMENT_SUCCESS_GROUP = "order-payment-success-group";
    }

    


    public static final class PaymentConsumerGroups {
        public static final String ORDER_CREATED_GROUP = "payment-order-created-group";
    }

    


    public static final class StockConsumerGroups {
        public static final String ORDER_CREATED_GROUP = "stock-order-created-group";
        public static final String ORDER_COMPLETED_GROUP = "stock-order-completed-group";
    }

    

    


    public static final class ProducerBindings {
        
        public static final String LOG_PRODUCER = "logProducer-out-0";

        
        public static final String ORDER_CREATED_PRODUCER = "orderCreatedProducer-out-0";
        public static final String ORDER_COMPLETED_PRODUCER = "orderCompletedProducer-out-0";

        
        public static final String PAYMENT_SUCCESS_PRODUCER = "paymentSuccessProducer-out-0";
    }

    


    public static final class ConsumerBindings {
        
        public static final String LOG_CONSUMER = "logConsumer-in-0";

        
        public static final String ORDER_CREATED_CONSUMER = "orderCreatedConsumer-in-0";
        public static final String ORDER_COMPLETED_CONSUMER = "orderCompletedConsumer-in-0";
        public static final String PAYMENT_SUCCESS_CONSUMER = "paymentSuccessConsumer-in-0";
    }

    

    


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
        public static final int DEFAULT_MAX_MESSAGE_SIZE = 4194304; 
        public static final int DEFAULT_COMPRESS_THRESHOLD = 4096; 
        public static final int DEFAULT_RETRY_TIMES = 2;
    }
}
