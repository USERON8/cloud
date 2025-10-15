package com.cloud.stock.task;

import com.cloud.stock.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 库存预警定时任务
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockAlertScheduledTask {

    private final StockAlertService stockAlertService;

    /**
     * 每小时检查一次低库存并发送预警通知
     * Cron: 每小时的第5分钟执行
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void checkLowStockAlerts() {
        log.info("⏰ 开始执行低库存预警定时任务");

        try {
            int alertCount = stockAlertService.checkAndSendLowStockAlerts();
            log.info("✅ 低库存预警定时任务执行完成, 预警商品数量: {}", alertCount);
        } catch (Exception e) {
            log.error("❌ 低库存预警定时任务执行失败", e);
        }
    }

    /**
     * 每天凌晨2点生成低库存统计报告
     * Cron: 每天2:00执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyLowStockReport() {
        log.info("⏰ 开始生成每日低库存统计报告");

        try {
            int alertCount = stockAlertService.checkAndSendLowStockAlerts();
            log.info("✅ 每日低库存统计报告生成完成, 预警商品数量: {}", alertCount);

            // TODO: 可以在这里生成报告并发送邮件给管理员
        } catch (Exception e) {
            log.error("❌ 每日低库存统计报告生成失败", e);
        }
    }
}
