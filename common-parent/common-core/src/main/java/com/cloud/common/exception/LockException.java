package com.cloud.common.exception;

















public class LockException extends BusinessException {




    public static final String LOCK_ACQUIRE_TIMEOUT = "LOCK_ACQUIRE_TIMEOUT";




    public static final String LOCK_RELEASE_FAILED = "LOCK_RELEASE_FAILED";




    public static final String LOCK_RENEW_FAILED = "LOCK_RENEW_FAILED";




    public static final String LOCK_OPERATION_ERROR = "LOCK_OPERATION_ERROR";






    public LockException(String message) {
        super(message);
    }







    public LockException(String code, String message) {
        super(message);


    }







    public LockException(String message, Throwable cause) {
        super(message, cause);
    }








    public LockException(String code, String message, Throwable cause) {
        super(message, cause);


    }








    public static LockException acquireTimeout(String lockKey, long waitTime) {
        return new LockException(LOCK_ACQUIRE_TIMEOUT,
                String.format("鑾峰彇鍒嗗竷寮忛攣瓒呮椂锛岄攣閿? %s, 绛夊緟鏃堕棿: %dms", lockKey, waitTime));
    }







    public static LockException releaseFailed(String lockKey) {
        return new LockException(LOCK_RELEASE_FAILED,
                String.format("閲婃斁鍒嗗竷寮忛攣澶辫触锛岄攣閿? %s", lockKey));
    }







    public static LockException renewFailed(String lockKey) {
        return new LockException(LOCK_RENEW_FAILED,
                String.format("缁湡鍒嗗竷寮忛攣澶辫触锛岄攣閿? %s", lockKey));
    }









    public static LockException operationError(String lockKey, String operation, Throwable cause) {
        return new LockException(LOCK_OPERATION_ERROR,
                String.format("鍒嗗竷寮忛攣鎿嶄綔寮傚父锛岄攣閿? %s, 鎿嶄綔: %s", lockKey, operation), cause);
    }
}
