package com.cloud.log.domain.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付事件Elasticsearch文档
 * 用于存储支付生命周期事件到ES
 * 基于阿里巴巴官方示例标准设计
 *
 * @author cloud
 * @date 2025/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "payment-events")
public class PaymentEventDocument {

    /**
     * 文档ID
     */
    @Id
    private String id;

    /**
     * 支付ID
     */
    @Field(type = FieldType.Keyword)
    private String paymentId;

    /**
     * 支付单号
     */
    @Field(type = FieldType.Keyword)
    private String paymentNo;

    /**
     * 业务订单ID
     */
    @Field(type = FieldType.Keyword)
    private String orderId;

    /**
     * 业务订单号
     */
    @Field(type = FieldType.Keyword)
    private String orderNo;

    /**
     * 用户ID
     */
    @Field(type = FieldType.Long)
    private Long userId;

    /**
     * 用户名
     */
    @Field(type = FieldType.Keyword)
    private String username;

    /**
     * 事件类型
     */
    @Field(type = FieldType.Keyword)
    private String eventType;

    /**
     * 消息标签
     */
    @Field(type = FieldType.Keyword)
    private String tag;

    /**
     * 追踪ID
     */
    @Field(type = FieldType.Keyword)
    private String traceId;

    /**
     * 支付状态
     */
    @Field(type = FieldType.Integer)
    private Integer paymentStatus;

    /**
     * 原支付状态
     */
    @Field(type = FieldType.Integer)
    private Integer oldPaymentStatus;

    /**
     * 支付金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal paymentAmount;

    /**
     * 实际支付金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal actualAmount;

    /**
     * 退款金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal refundAmount;

    /**
     * 手续费
     */
    @Field(type = FieldType.Double)
    private BigDecimal feeAmount;

    /**
     * 支付方式
     */
    @Field(type = FieldType.Integer)
    private Integer paymentMethod;

    /**
     * 支付渠道
     */
    @Field(type = FieldType.Keyword)
    private String paymentChannel;

    /**
     * 第三方支付单号（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String thirdPartyTxnId;

    /**
     * 第三方交易状态
     */
    @Field(type = FieldType.Keyword)
    private String thirdPartyStatus;

    /**
     * 支付时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paymentTime;

    /**
     * 支付完成时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedTime;

    /**
     * 退款时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime refundTime;

    /**
     * 退款原因
     */
    @Field(type = FieldType.Text)
    private String refundReason;

    /**
     * 失败原因
     */
    @Field(type = FieldType.Text)
    private String failureReason;

    /**
     * 失败错误码
     */
    @Field(type = FieldType.Keyword)
    private String failureCode;

    /**
     * 货币类型
     */
    @Field(type = FieldType.Keyword)
    private String currency;

    /**
     * 支付IP地址（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String paymentIp;

    /**
     * 设备信息（脱敏后）
     */
    @Field(type = FieldType.Text)
    private String deviceInfo;

    /**
     * 用户代理（脱敏后）
     */
    @Field(type = FieldType.Text)
    private String userAgent;

    /**
     * 支付来源
     */
    @Field(type = FieldType.Integer)
    private Integer paymentSource;

    /**
     * 业务类型
     */
    @Field(type = FieldType.Integer)
    private Integer businessType;

    /**
     * 通知URL（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String notifyUrl;

    /**
     * 返回URL（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String returnUrl;

    /**
     * 商品描述
     */
    @Field(type = FieldType.Text)
    private String productDescription;

    /**
     * 备注信息
     */
    @Field(type = FieldType.Text)
    private String remark;

    /**
     * 支付创建时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paymentCreateTime;

    /**
     * 支付更新时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paymentUpdateTime;

    /**
     * 事件发生时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eventTime;

    /**
     * 操作人ID
     */
    @Field(type = FieldType.Long)
    private Long operatorId;

    /**
     * 操作人名称
     */
    @Field(type = FieldType.Keyword)
    private String operatorName;

    /**
     * 扩展字段
     */
    @Field(type = FieldType.Text)
    private String extendData;

    /**
     * 消息时间戳
     */
    @Field(type = FieldType.Long)
    private Long messageTimestamp;

    /**
     * 处理时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processTime;
}
