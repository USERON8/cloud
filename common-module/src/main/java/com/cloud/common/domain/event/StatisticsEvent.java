package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统计事件
 * 用于各种统计数据收集的统一事件模型
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsEvent {

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件类型
     * PRODUCT_VIEW: 商品浏览
     * PRODUCT_SALES: 商品销售
     * USER_BEHAVIOR: 用户行为
     * SEARCH: 搜索行为
     * ORDER: 订单统计
     * PAYMENT: 支付统计
     * SHOP_VIEW: 店铺访问
     * CATEGORY_VIEW: 分类访问
     */
    private String eventType;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 行为类型
     * VIEW: 浏览
     * CLICK: 点击
     * ADD_CART: 加入购物车
     * PURCHASE: 购买
     * SHARE: 分享
     * FAVORITE: 收藏
     */
    private String actionType;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 统计值
     */
    private Long value;

    /**
     * 来源
     * WEB: 网页
     * APP: 移动应用
     * MINI_PROGRAM: 小程序
     * API: API接口
     */
    private String source;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 设备类型
     * PC: 电脑
     * MOBILE: 手机
     * TABLET: 平板
     */
    private String deviceType;

    /**
     * 操作系统
     */
    private String operatingSystem;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 地理位置 - 省份
     */
    private String province;

    /**
     * 地理位置 - 城市
     */
    private String city;

    /**
     * 持续时间（毫秒）
     */
    private Long duration;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;

    /**
     * 结果数量
     */
    private Integer resultCount;

    /**
     * 订单状态
     */
    private String orderStatus;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 支付状态
     */
    private String paymentStatus;

    /**
     * 事件时间
     */
    private LocalDateTime eventTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 页面路径
     */
    private String pagePath;

    /**
     * 引用页面
     */
    private String referrer;

    /**
     * 扩展属性
     */
    private Map<String, Object> extraData;

    /**
     * 备注
     */
    private String remark;
}
