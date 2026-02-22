package com.cloud.common.exception;









public class MessageConsumeException extends BusinessException {

    




    public MessageConsumeException(String message) {
        super(message);
    }

    





    public MessageConsumeException(String message, Throwable cause) {
        super(message, cause);
    }

    






    public MessageConsumeException(String topic, String traceId, Throwable cause) {
        super(String.format("娑堟伅娑堣垂澶辫触 - Topic: %s, TraceId: %s", topic, traceId), cause);
    }
}
