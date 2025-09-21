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
 * 订单事件Elasticsearch文档
 * 用于存储订单生命周期事件到ES
 * 基于阿里巴巴官方示例标准设计
 *
 * @author cloud
 * @date 2025/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "order-events", createIndex = true)
public class OrderEventDocument {

    /**
     * 文档ID
     */
    @Id
    private String id;

    /**
     * 订单ID
     */
    @Field(type = FieldType.Long)
    private Long orderId;

    /**
     * 订单编号
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
     * 订单状态
     */
    @Field(type = FieldType.Integer)
    private Integer orderStatus;

    /**
     * 原订单状态
     */
    @Field(type = FieldType.Integer)
    private Integer oldOrderStatus;

    /**
     * 订单总金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal paidAmount;

    /**
     * 优惠金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal discountAmount;

    /**
     * 运费
     */
    @Field(type = FieldType.Double)
    private BigDecimal shippingFee;

    /**
     * 收货人姓名（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String receiverName;

    /**
     * 收货人电话（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String receiverPhone;

    /**
     * 收货地址（脱敏后）
     */
    @Field(type = FieldType.Text)
    private String receiverAddress;

    /**
     * 支付方式
     */
    @Field(type = FieldType.Integer)
    private Integer paymentMethod;

    /**
     * 支付时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paymentTime;

    /**
     * 发货时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime shippingTime;

    /**
     * 完成时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedTime;

    /**
     * 取消时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime cancelledTime;

    /**
     * 取消原因
     */
    @Field(type = FieldType.Text)
    private String cancelReason;

    /**
     * 退款金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal refundAmount;

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
     * 物流公司
     */
    @Field(type = FieldType.Keyword)
    private String logisticsCompany;

    /**
     * 物流单号
     */
    @Field(type = FieldType.Keyword)
    private String trackingNumber;

    /**
     * 订单来源
     */
    @Field(type = FieldType.Integer)
    private Integer orderSource;

    /**
     * 备注信息
     */
    @Field(type = FieldType.Text)
    private String remark;

    /**
     * 订单创建时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime orderCreateTime;

    /**
     * 订单更新时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime orderUpdateTime;

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
