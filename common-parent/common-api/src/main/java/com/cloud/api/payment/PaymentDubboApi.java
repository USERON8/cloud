package com.cloud.api.payment;

import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;

/**
 * 支付服务内部调用契约。
 *
 * <p>订单服务通过该接口创建支付单、查询支付状态和发起退款；支付回调由支付服务内部落地后再反向通知订单状态。
 */
public interface PaymentDubboApi {

  /** 创建支付单。 */
  Long createPaymentOrder(PaymentOrderCommandDTO command);

  /** 按支付单号查询支付单。 */
  PaymentOrderVO getPaymentOrderByNo(String paymentNo);

  /** 按主订单号和子订单号查询支付单。 */
  PaymentOrderVO getPaymentOrderByOrderNo(String mainOrderNo, String subOrderNo);

  /** 处理第三方支付回调后的支付状态变更。 */
  Boolean handlePaymentCallback(PaymentCallbackCommandDTO command);

  /** 创建退款单。 */
  Long createRefund(PaymentRefundCommandDTO command);

  /** 按退款单号查询退款单。 */
  PaymentRefundVO getRefundByNo(String refundNo);

  /** 取消退款单并记录原因。 */
  Boolean cancelRefund(String refundNo, String reason);
}
