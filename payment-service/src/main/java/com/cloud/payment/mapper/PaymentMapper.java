package com.cloud.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.payment.module.entity.Payment;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;









public interface PaymentMapper extends BaseMapper<Payment> {

    

    







    int updateStatusToSuccess(@Param("paymentId") Long paymentId,
                              @Param("transactionId") String transactionId);

    







    int updateStatusToFailed(@Param("paymentId") Long paymentId,
                             @Param("failureReason") String failureReason);

    







    int updateStatusToRefunded(@Param("paymentId") Long paymentId,
                               @Param("refundTransactionId") String refundTransactionId);

    





    Payment selectByTraceId(@Param("traceId") String traceId);

    






    Payment selectByOrderIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

    






    Payment selectByIdForUpdate(@Param("paymentId") Long paymentId);

    










    int insertPaymentIdempotent(@Param("orderId") Long orderId,
                                @Param("userId") Long userId,
                                @Param("amount") BigDecimal amount,
                                @Param("channel") Integer channel,
                                @Param("traceId") String traceId);

    






    java.util.List<Payment> selectBatchByIds(@Param("paymentIds") java.util.List<Long> paymentIds);
}




