package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知事件
 * 用于各种通知消息的统一事件模型
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    /**
     * 通知ID
     */
    private String notificationId;

    /**
     * 通知类型
     * EMAIL: 邮件通知
     * SMS: 短信通知
     * IN_APP: 站内消息
     * PUSH: 推送通知
     * WECHAT: 微信通知
     * SYSTEM: 系统通知
     */
    private String notificationType;

    /**
     * 接收者
     * 邮件：邮箱地址
     * 短信：手机号码
     * 站内消息：用户ID
     * 推送：设备令牌
     */
    private String recipient;

    /**
     * 通知主题/标题
     */
    private String subject;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 消息类型
     * SYSTEM: 系统消息
     * ORDER: 订单消息
     * PAYMENT: 支付消息
     * PROMOTION: 促销消息
     * REMINDER: 提醒消息
     */
    private String messageType;

    /**
     * 模板代码
     */
    private String templateCode;

    /**
     * 模板参数
     */
    private Map<String, Object> templateParams;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 优先级
     * LOW: 低优先级
     * NORMAL: 普通优先级
     * HIGH: 高优先级
     * URGENT: 紧急优先级
     */
    private String priority;

    /**
     * 状态
     * PENDING: 待发送
     * SENT: 已发送
     * DELIVERED: 已送达
     * FAILED: 发送失败
     * CANCELLED: 已取消
     */
    private String status;

    /**
     * 发送渠道
     */
    private String channel;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;

    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 扩展属性
     */
    private Map<String, Object> extraData;

    /**
     * 备注
     */
    private String remark;
}
