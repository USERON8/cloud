package com.cloud.payment.module.dto;

import lombok.Data;

import java.math.BigDecimal;







@Data
public class AlipayNotifyRequest {

    


    private String notifyTime;

    


    private String notifyType;

    


    private String notifyId;

    


    private String appId;

    


    private String charset;

    


    private String version;

    


    private String signType;

    


    private String sign;

    


    private String tradeNo;

    


    private String outTradeNo;

    


    private String outBizNo;

    


    private String buyerLogonId;

    


    private String buyerId;

    


    private String sellerId;

    


    private String sellerEmail;

    


    private String tradeStatus;

    


    private BigDecimal totalAmount;

    


    private BigDecimal receiptAmount;

    


    private BigDecimal invoiceAmount;

    


    private BigDecimal buyerPayAmount;

    


    private BigDecimal pointAmount;

    


    private BigDecimal refundFee;

    


    private String subject;

    


    private String body;

    


    private String gmtCreate;

    


    private String gmtPayment;

    


    private String gmtRefund;

    


    private String gmtClose;

    


    private String fundBillList;

    


    private String passbackParams;

    


    private String voucherDetailList;
}
