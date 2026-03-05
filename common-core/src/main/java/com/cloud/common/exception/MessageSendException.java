package com.cloud.common.exception;









public class MessageSendException extends BusinessException {

    




    public MessageSendException(String message) {
        super(message);
    }

    





    public MessageSendException(String message, Throwable cause) {
        super(message, cause);
    }

    






    public MessageSendException(String topic, String traceId, Throwable cause) {
        super(String.format("娑堟伅鍙戦€佸け璐?- Topic: %s, TraceId: %s", topic, traceId), cause);
    }
}
