package com.cloud.payment.controller;

import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.result.Result;
import com.cloud.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * 支付业务控制器
 * 演示分布式锁在支付业务场景中的应用
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payment/business")
@RequiredArgsConstructor
@Tag(name = "支付业务管理", description = "支付业务相关接口，集成分布式锁保护")
public class PaymentBusinessController {

    private final PaymentService paymentService;

    /**
     * 处理支付请求 - 使用分布式锁防止重复支付
     */
    @PostMapping("/process")
    @Operation(summary = "处理支付", description = "处理支付请求，使用分布式锁防止重复支付")
    @DistributedLock(
            key = "'payment:process:order:' + #orderId + ':user:' + #userId",
            waitTime = 5,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "支付处理中，请勿重复提交"
    )
    public Result<String> processPayment(
            @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "支付金额") @RequestParam BigDecimal amount,
            @Parameter(description = "支付方式") @RequestParam String paymentMethod,
            @Parameter(description = "支付流水号") @RequestParam String traceId) {

        log.info("💳 处理支付请求 - 订单ID: {}, 用户ID: {}, 金额: {}, 方式: {}, 流水号: {}",
                orderId, userId, amount, paymentMethod, traceId);

        try {
            // 模拟支付处理逻辑
            Thread.sleep(2000); // 模拟支付网关调用耗时

            // 这里应该调用实际的支付服务方法
            // boolean result = paymentService.processPayment(orderId, userId, amount, paymentMethod, traceId);

            // 模拟支付成功
            boolean result = true;

            if (result) {
                String successMsg = String.format("支付成功 - 订单ID: %d, 金额: %s, 流水号: %s",
                        orderId, amount, traceId);
                log.info("✅ {}", successMsg);
                return Result.success(successMsg, "支付成功");
            } else {
                String failMsg = String.format("支付失败 - 订单ID: %d, 流水号: %s", orderId, traceId);
                log.warn("❌ {}", failMsg);
                return Result.error(failMsg);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ 支付处理被中断 - 订单ID: {}, 流水号: {}", orderId, traceId);
            return Result.error("支付处理被中断");
        } catch (Exception e) {
            log.error("❌ 支付处理异常 - 订单ID: {}, 流水号: {}", orderId, traceId, e);
            return Result.error("支付处理失败: " + e.getMessage());
        }
    }

    /**
     * 支付退款 - 使用分布式锁确保退款幂等性
     */
    @PostMapping("/refund")
    @Operation(summary = "支付退款", description = "处理支付退款，使用分布式锁确保幂等性")
    @DistributedLock(
            key = "'payment:refund:' + #paymentId + ':' + #refundTraceId",
            waitTime = 3,
            leaseTime = 20,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "退款处理中，请勿重复提交"
    )
    public Result<String> refundPayment(
            @Parameter(description = "支付ID") @RequestParam Long paymentId,
            @Parameter(description = "退款金额") @RequestParam BigDecimal refundAmount,
            @Parameter(description = "退款原因") @RequestParam String refundReason,
            @Parameter(description = "退款流水号") @RequestParam String refundTraceId) {

        log.info("💰 处理退款请求 - 支付ID: {}, 退款金额: {}, 原因: {}, 流水号: {}",
                paymentId, refundAmount, refundReason, refundTraceId);

        try {
            // 模拟退款处理逻辑
            Thread.sleep(1500); // 模拟退款网关调用耗时

            // 这里应该调用实际的退款服务方法
            // boolean result = paymentService.refundPayment(paymentId, refundAmount, refundReason, refundTraceId);

            // 模拟退款成功
            boolean result = true;

            if (result) {
                String successMsg = String.format("退款成功 - 支付ID: %d, 退款金额: %s, 流水号: %s",
                        paymentId, refundAmount, refundTraceId);
                log.info("✅ {}", successMsg);
                return Result.success(successMsg, "退款成功");
            } else {
                String failMsg = String.format("退款失败 - 支付ID: %d, 流水号: %s", paymentId, refundTraceId);
                log.warn("❌ {}", failMsg);
                return Result.error(failMsg);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ 退款处理被中断 - 支付ID: {}, 流水号: {}", paymentId, refundTraceId);
            return Result.error("退款处理被中断");
        } catch (Exception e) {
            log.error("❌ 退款处理异常 - 支付ID: {}, 流水号: {}", paymentId, refundTraceId, e);
            return Result.error("退款处理失败: " + e.getMessage());
        }
    }

    /**
     * 批量支付查询 - 使用读锁允许并发查询
     */
    @GetMapping("/batch-query")
    @Operation(summary = "批量支付查询", description = "批量查询支付状态，使用读锁允许并发访问")
    @DistributedLock(
            key = "'payment:query:batch:' + T(String).join(',', #paymentIds)",
            lockType = DistributedLock.LockType.READ,
            waitTime = 2,
            leaseTime = 5,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    public Result<String> batchQueryPayments(
            @Parameter(description = "支付ID列表") @RequestParam java.util.List<Long> paymentIds) {

        log.info("🔍 批量查询支付状态 - 支付数量: {}", paymentIds.size());

        try {
            // 模拟批量查询逻辑
            Thread.sleep(300);

            int successCount = 0;
            int failedCount = 0;
            int pendingCount = 0;

            // 模拟查询结果统计
            for (Long paymentId : paymentIds) {
                // 这里应该调用实际的查询方法
                // PaymentStatus status = paymentService.getPaymentStatus(paymentId);

                // 模拟随机状态
                int randomStatus = (int) (Math.random() * 3);
                switch (randomStatus) {
                    case 0 -> pendingCount++;
                    case 1 -> successCount++;
                    case 2 -> failedCount++;
                }
            }

            String result = String.format("批量查询完成 - 总数: %d, 成功: %d, 失败: %d, 处理中: %d",
                    paymentIds.size(), successCount, failedCount, pendingCount);

            log.info("✅ {}", result);

            return Result.success(result, "查询完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("查询被中断");
        } catch (Exception e) {
            log.error("❌ 批量查询支付状态失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 支付对账 - 使用写锁确保对账数据一致性
     */
    @PostMapping("/reconciliation")
    @Operation(summary = "支付对账", description = "执行支付对账，使用写锁确保数据一致性")
    @DistributedLock(
            key = "'payment:reconciliation:' + #reconciliationDate",
            lockType = DistributedLock.LockType.WRITE,
            waitTime = 10,
            leaseTime = 60,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "支付对账正在进行中"
    )
    public Result<String> reconcilePayments(
            @Parameter(description = "对账日期") @RequestParam String reconciliationDate,
            @Parameter(description = "对账类型") @RequestParam String reconciliationType) {

        log.info("📊 开始支付对账 - 日期: {}, 类型: {}", reconciliationDate, reconciliationType);

        try {
            // 模拟对账处理逻辑
            Thread.sleep(5000); // 模拟对账耗时

            // 模拟对账结果
            int totalCount = 1000;
            int matchedCount = 995;
            int unmatchedCount = 5;

            String result = String.format("对账完成 - 日期: %s, 总笔数: %d, 匹配: %d, 不匹配: %d",
                    reconciliationDate, totalCount, matchedCount, unmatchedCount);

            log.info("✅ {}", result);

            return Result.success(result, "对账完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ 支付对账被中断 - 日期: {}", reconciliationDate);
            return Result.error("对账被中断");
        } catch (Exception e) {
            log.error("❌ 支付对账失败 - 日期: {}", reconciliationDate, e);
            return Result.error("对账失败: " + e.getMessage());
        }
    }

    /**
     * 支付风控检查 - 使用快速失败策略
     */
    @PostMapping("/risk-check")
    @Operation(summary = "支付风控检查", description = "执行支付风控检查，使用快速失败策略")
    @DistributedLock(
            key = "'payment:risk:user:' + #userId",
            waitTime = 0,
            leaseTime = 3,
            timeUnit = TimeUnit.SECONDS,
            failStrategy = DistributedLock.LockFailStrategy.FAIL_FAST,
            failMessage = "风控检查系统繁忙，请稍后再试"
    )
    public Result<String> riskCheck(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "支付金额") @RequestParam BigDecimal amount,
            @Parameter(description = "支付方式") @RequestParam String paymentMethod) {

        log.info("🛡️ 支付风控检查 - 用户ID: {}, 金额: {}, 方式: {}", userId, amount, paymentMethod);

        try {
            // 模拟风控检查逻辑
            Thread.sleep(200);

            // 模拟风控规则检查
            boolean riskPassed = true;
            String riskReason = "";

            // 金额风控
            if (amount.compareTo(new BigDecimal("10000")) > 0) {
                riskPassed = false;
                riskReason = "单笔金额超过限额";
            }

            String result;
            if (riskPassed) {
                result = String.format("风控检查通过 - 用户ID: %d, 金额: %s", userId, amount);
                log.info("✅ {}", result);
            } else {
                result = String.format("风控检查不通过 - 用户ID: %d, 原因: %s", userId, riskReason);
                log.warn("⚠️ {}", result);
            }

            return Result.success(result, riskPassed ? "检查通过" : "检查不通过");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("风控检查被中断");
        } catch (Exception e) {
            log.error("❌ 支付风控检查失败 - 用户ID: {}", userId, e);
            return Result.error("风控检查失败: " + e.getMessage());
        }
    }

    /**
     * 支付渠道切换 - 使用公平锁确保按顺序处理
     */
    @PostMapping("/channel-switch")
    @Operation(summary = "支付渠道切换", description = "切换支付渠道，使用公平锁确保按顺序处理")
    @DistributedLock(
            key = "'payment:channel:switch:' + #fromChannel + ':' + #toChannel",
            lockType = DistributedLock.LockType.FAIR,
            waitTime = 5,
            leaseTime = 15,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "支付渠道切换获取锁失败"
    )
    public Result<String> switchPaymentChannel(
            @Parameter(description = "原渠道") @RequestParam String fromChannel,
            @Parameter(description = "目标渠道") @RequestParam String toChannel,
            @Parameter(description = "切换原因") @RequestParam String reason) {

        log.info("🔄 支付渠道切换 - 从 {} 切换到 {}, 原因: {}", fromChannel, toChannel, reason);

        try {
            // 模拟渠道切换逻辑
            Thread.sleep(1000);

            String result = String.format("支付渠道切换完成 - %s -> %s, 原因: %s",
                    fromChannel, toChannel, reason);

            log.info("✅ {}", result);

            return Result.success(result, "切换完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("渠道切换被中断");
        } catch (Exception e) {
            log.error("❌ 支付渠道切换失败 - {} -> {}", fromChannel, toChannel, e);
            return Result.error("渠道切换失败: " + e.getMessage());
        }
    }
}
