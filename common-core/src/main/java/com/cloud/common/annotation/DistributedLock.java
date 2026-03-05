package com.cloud.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;









@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    






    String key();

    





    String prefix() default "";

    





    long waitTime() default 3;

    





    long leaseTime() default 10;

    




    TimeUnit timeUnit() default TimeUnit.SECONDS;

    




    LockType lockType() default LockType.REENTRANT;

    




    LockFailStrategy failStrategy() default LockFailStrategy.THROW_EXCEPTION;

    





    String failMessage() default "鑾峰彇鍒嗗竷寮忛攣澶辫触";

    




    boolean autoRelease() default true;

    


    enum LockType {
        


        REENTRANT,

        


        FAIR,

        


        READ,

        


        WRITE,

        


        RED_LOCK
    }

    


    enum LockFailStrategy {
        


        THROW_EXCEPTION,

        


        RETURN_NULL,

        


        RETURN_DEFAULT,

        


        FAIL_FAST
    }
}
