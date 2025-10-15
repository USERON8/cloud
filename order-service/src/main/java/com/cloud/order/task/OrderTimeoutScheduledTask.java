package com.cloud.order.task;

import com.cloud.order.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时处理定时任务
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduledTask {

    private final OrderTimeoutService orderTimeoutService;

    /**
     * 每5分钟检查一次超时未支付订单
     * Cron: 每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void checkTimeoutOrders() {
        log.info("⏰ 开始执行订单超时检查定时任务");

        try {
            int cancelCount = orderTimeoutService.checkAndHandleTimeoutOrders();
            log.info("✅ 订单超时检查定时任务执行完成, 取消订单数: {}", cancelCount);
        } catch (Exception e) {
            log.error("❌ 订单超时检查定时任务执行失败", e);
        }
    }

    /**
     * 每天凌晨1点生成超时订单统计报告
     * Cron: 每天1:00执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void generateTimeoutOrderReport() {
        log.info("⏰ 开始生成每日超时订单统计报告");

        try {
            // TODO: 统计昨天的超时订单数据
            // TODO: 生成报告并发送给管理员

            log.info("✅ 每日超时订单统计报告生成完成");
        } catch (Exception e) {
            log.error("❌ 每日超时订单统计报告生成失败", e);
        }
    }
}
