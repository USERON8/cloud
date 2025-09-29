package com.cloud.log.messaging.consumer;

import com.cloud.common.domain.event.base.BaseBusinessLogEvent;
import com.cloud.common.domain.event.order.OrderOperationLogEvent;
import com.cloud.common.domain.event.payment.PaymentOperationLogEvent;
import com.cloud.common.domain.event.product.ProductChangeLogEvent;
import com.cloud.common.domain.event.product.ShopChangeLogEvent;
import com.cloud.common.domain.event.user.UserChangeLogEvent;
import com.cloud.common.exception.MessageConsumeException;
import com.cloud.log.service.UnifiedBusinessLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * 统一业务日志消费者
 * 负责消费各种类型的业务日志事件，统一处理和存储
 *
 * @author CloudDevAgent
 * @since 2025-09-27
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class UnifiedBusinessLogConsumer {

    private final UnifiedBusinessLogService businessLogService;

    /**
     * 统一业务日志消费者函数
     * 对应绑定名称: businessLog-in-0
     */
    @Bean("businessLogMessageConsumer")
    public Consumer<Message<BaseBusinessLogEvent>> businessLogConsumer() {
        return message -> {
            try {
                BaseBusinessLogEvent event = message.getPayload();
                String logType = event.getLogType();
                String traceId = event.getTraceId();
                String serviceName = event.getServiceName();
                String operation = event.getOperation();

                log.info("📥 接收到统一业务日志消息 - 类型: {}, 服务: {}, 操作: {}, 业务ID: {}, 追踪ID: {}",
                        logType, serviceName, operation, event.getBusinessId(), traceId);

                // 1. 幂等性检查
                if (businessLogService.isLogProcessed(event.getLogId())) {
                    log.warn("⚠️ 业务日志已处理，跳过 - 日志ID: {}, 追踪ID: {}", event.getLogId(), traceId);
                    return;
                }

                // 2. 数据脱敏处理
                BaseBusinessLogEvent sanitizedEvent = sanitizeBusinessLogEvent(event);

                // 3. 根据日志类型分别处理
                boolean saved = processBusinessLogEvent(sanitizedEvent);

                if (saved) {
                    log.info("✅ 业务日志保存成功 - 类型: {}, 服务: {}, 操作: {}, 业务ID: {}, 追踪ID: {}",
                            logType, serviceName, operation, event.getBusinessId(), traceId);

                    // 4. 标记已处理
                    businessLogService.markLogProcessed(event.getLogId());
                } else {
                    log.error("❌ 业务日志保存失败 - 类型: {}, 服务: {}, 操作: {}, 业务ID: {}, 追踪ID: {}",
                            logType, serviceName, operation, event.getBusinessId(), traceId);
                    throw new MessageConsumeException("业务日志保存失败", null);
                }

            } catch (Exception e) {
                log.error("❌ 处理统一业务日志消息时发生异常: {}", e.getMessage(), e);
                throw new MessageConsumeException("处理统一业务日志消息异常", e);
            }
        };
    }

    /**
     * 根据日志类型处理业务日志事件
     */
    private boolean processBusinessLogEvent(BaseBusinessLogEvent event) {
        return switch (event.getLogType()) {
            case "USER_CHANGE" -> businessLogService.saveUserChangeLog((UserChangeLogEvent) event);
            case "PRODUCT_CHANGE" -> businessLogService.saveProductChangeLog((ProductChangeLogEvent) event);
            case "SHOP_CHANGE" -> businessLogService.saveShopChangeLog((ShopChangeLogEvent) event);
            case "ORDER_OPERATION" -> businessLogService.saveOrderOperationLog((OrderOperationLogEvent) event);
            case "PAYMENT_OPERATION" -> businessLogService.savePaymentOperationLog((PaymentOperationLogEvent) event);
            default -> {
                log.warn("未知的业务日志类型: {}, 使用通用处理方式", event.getLogType());
                yield businessLogService.saveGenericBusinessLog(event);
            }
        };
    }

    /**
     * 业务日志事件数据脱敏处理
     */
    private BaseBusinessLogEvent sanitizeBusinessLogEvent(BaseBusinessLogEvent event) {
        // 对用户名进行脱敏
        if (event.getUserName() != null) {
            event.setUserName(sanitizeUserName(event.getUserName()));
        }

        // 对变更前后数据进行脱敏
        if (event.getBeforeData() != null) {
            event.setBeforeData(sanitizeJsonData(event.getBeforeData()));
        }
        if (event.getAfterData() != null) {
            event.setAfterData(sanitizeJsonData(event.getAfterData()));
        }

        // 根据具体事件类型进行特殊脱敏处理
        if (event instanceof UserChangeLogEvent userEvent) {
            return sanitizeUserChangeLogEvent(userEvent);
        } else if (event instanceof ProductChangeLogEvent productEvent) {
            return sanitizeProductChangeLogEvent(productEvent);
        } else if (event instanceof ShopChangeLogEvent shopEvent) {
            return sanitizeShopChangeLogEvent(shopEvent);
        }

        return event;
    }

    /**
     * 脱敏用户变更日志事件
     */
    private UserChangeLogEvent sanitizeUserChangeLogEvent(UserChangeLogEvent event) {
        if (event.getEmail() != null) {
            event.setEmail(sanitizeEmail(event.getEmail()));
        }
        if (event.getPhone() != null) {
            event.setPhone(sanitizePhone(event.getPhone()));
        }
        if (event.getIpAddress() != null) {
            event.setIpAddress(sanitizeIp(event.getIpAddress()));
        }
        return event;
    }

    /**
     * 脱敏商品变更日志事件
     */
    private ProductChangeLogEvent sanitizeProductChangeLogEvent(ProductChangeLogEvent event) {
        // 商品信息一般不需要特殊脱敏，保持原样
        return event;
    }

    /**
     * 脱敏店铺变更日志事件
     */
    private ShopChangeLogEvent sanitizeShopChangeLogEvent(ShopChangeLogEvent event) {
        if (event.getContactPhone() != null) {
            event.setContactPhone(sanitizePhone(event.getContactPhone()));
        }
        return event;
    }

    /**
     * 脱敏用户名
     */
    private String sanitizeUserName(String userName) {
        if (userName == null || userName.length() <= 2) {
            return userName;
        }
        return userName.charAt(0) + "***" + userName.charAt(userName.length() - 1);
    }

    /**
     * 脱敏邮箱
     */
    private String sanitizeEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return email;
        }
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + "@" + domain;
    }

    /**
     * 脱敏手机号
     */
    private String sanitizePhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 脱敏IP地址
     */
    private String sanitizeIp(String ip) {
        if (ip == null || !ip.contains(".")) {
            return ip;
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".***.***";
        }
        return ip;
    }

    /**
     * 脱敏JSON数据
     */
    private String sanitizeJsonData(String jsonData) {
        if (jsonData == null) {
            return null;
        }
        // 对JSON数据中的敏感字段进行脱敏
        return jsonData.replaceAll("\"(password|pwd|token|secret)\"\\s*:\\s*\"[^\"]*\"", "\"$1\":\"***\"")
                .replaceAll("\"(phone|mobile)\"\\s*:\\s*\"\\d{11}\"", "\"$1\":\"***\"")
                .replaceAll("\"(email)\"\\s*:\\s*\"[^\"]*@[^\"]*\"", "\"$1\":\"***@***.com\"")
                .replaceAll("\"(idCard|cardNo)\"\\s*:\\s*\"\\d+\"", "\"$1\":\"***\"");
    }
}
