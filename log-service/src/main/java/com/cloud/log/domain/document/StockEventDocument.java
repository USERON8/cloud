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
 * 库存事件Elasticsearch文档
 * 用于存储库存生命周期事件到ES
 * 基于阿里巴巴官方示例标准设计
 *
 * @author cloud
 * @date 2025/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "stock-events", createIndex = true)
public class StockEventDocument {

    /**
     * 文档ID
     */
    @Id
    private String id;

    /**
     * 库存记录ID
     */
    @Field(type = FieldType.Long)
    private Long stockId;

    /**
     * 商品ID
     */
    @Field(type = FieldType.Long)
    private Long productId;

    /**
     * 商品编码
     */
    @Field(type = FieldType.Keyword)
    private String productCode;

    /**
     * 商品名称
     */
    @Field(type = FieldType.Keyword)
    private String productName;

    /**
     * SKU ID
     */
    @Field(type = FieldType.Long)
    private Long skuId;

    /**
     * SKU编码
     */
    @Field(type = FieldType.Keyword)
    private String skuCode;

    /**
     * 仓库ID
     */
    @Field(type = FieldType.Long)
    private Long warehouseId;

    /**
     * 仓库编码
     */
    @Field(type = FieldType.Keyword)
    private String warehouseCode;

    /**
     * 仓库名称
     */
    @Field(type = FieldType.Keyword)
    private String warehouseName;

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
     * 变更前库存数量
     */
    @Field(type = FieldType.Integer)
    private Integer beforeQuantity;

    /**
     * 变更后库存数量
     */
    @Field(type = FieldType.Integer)
    private Integer afterQuantity;

    /**
     * 变更数量
     */
    @Field(type = FieldType.Integer)
    private Integer changeQuantity;

    /**
     * 可用库存数量
     */
    @Field(type = FieldType.Integer)
    private Integer availableQuantity;

    /**
     * 锁定库存数量
     */
    @Field(type = FieldType.Integer)
    private Integer lockedQuantity;

    /**
     * 预扣库存数量
     */
    @Field(type = FieldType.Integer)
    private Integer reservedQuantity;

    /**
     * 变更类型
     */
    @Field(type = FieldType.Integer)
    private Integer changeType;

    /**
     * 业务类型
     */
    @Field(type = FieldType.Integer)
    private Integer businessType;

    /**
     * 关联业务单号
     */
    @Field(type = FieldType.Keyword)
    private String businessNo;

    /**
     * 关联订单ID
     */
    @Field(type = FieldType.Long)
    private Long orderId;

    /**
     * 关联订单号
     */
    @Field(type = FieldType.Keyword)
    private String orderNo;

    /**
     * 供应商ID
     */
    @Field(type = FieldType.Long)
    private Long supplierId;

    /**
     * 供应商名称（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String supplierName;

    /**
     * 客户ID
     */
    @Field(type = FieldType.Long)
    private Long customerId;

    /**
     * 客户名称（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String customerName;

    /**
     * 单价
     */
    @Field(type = FieldType.Double)
    private BigDecimal unitPrice;

    /**
     * 总金额
     */
    @Field(type = FieldType.Double)
    private BigDecimal totalAmount;

    /**
     * 成本价
     */
    @Field(type = FieldType.Double)
    private BigDecimal costPrice;

    /**
     * 批次号
     */
    @Field(type = FieldType.Keyword)
    private String batchNo;

    /**
     * 生产日期
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime productionDate;

    /**
     * 过期日期
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expirationDate;

    /**
     * 变更原因
     */
    @Field(type = FieldType.Text)
    private String changeReason;

    /**
     * 变更备注
     */
    @Field(type = FieldType.Text)
    private String changeRemark;

    /**
     * 库存状态
     */
    @Field(type = FieldType.Integer)
    private Integer stockStatus;

    /**
     * 是否自动处理
     */
    @Field(type = FieldType.Boolean)
    private Boolean autoProcessed;

    /**
     * 库存创建时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime stockCreateTime;

    /**
     * 库存更新时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime stockUpdateTime;

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
     * 操作类型
     */
    @Field(type = FieldType.Integer)
    private Integer operationType;

    /**
     * 操作来源IP（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String operationIp;

    /**
     * 操作来源系统
     */
    @Field(type = FieldType.Keyword)
    private String operationSource;

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
