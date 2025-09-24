package com.cloud.order.controller;

import com.cloud.common.result.Result;
import com.cloud.order.dto.SimpleOrderCreateDTO;
import com.cloud.order.service.SimpleOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 简化订单控制器
 * 专门处理单商品订单的创建和管理
 * 简化业务流程，专注于演示完整的事件驱动流程
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/order/simple")
@RequiredArgsConstructor
@Tag(name = "简化订单接口", description = "专门处理单商品订单的创建和管理，演示事件驱动流程")
public class SimpleOrderController {

    private final SimpleOrderService simpleOrderService;

    /**
     * 创建单商品订单
     * 简化版订单创建流程，支持单商品快速下单
     *
     * @param orderCreateDTO 简化订单创建信息
     * @param currentUserId  当前用户ID（从请求头获取）
     * @return 创建结果
     */
    @PostMapping("/create")
    @Operation(summary = "创建单商品订单", description = "简化版订单创建，支持单商品快速下单，自动触发库存冻结和支付流程")
    public Result<String> createSimpleOrder(
            @Parameter(description = "简化订单创建信息", required = true)
            @Valid @RequestBody SimpleOrderCreateDTO orderCreateDTO,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-ID", defaultValue = "1001") String currentUserId) {

        try {
            log.info("🚀 开始创建单商品订单 - 用户: {}, 商品: {}, 数量: {}",
                    currentUserId, orderCreateDTO.getProductId(), orderCreateDTO.getQuantity());

            // 调用简化订单服务创建订单
            Long orderId = simpleOrderService.createSimpleOrder(orderCreateDTO, currentUserId);

            log.info("✅ 单商品订单创建成功 - 订单ID: {}, 用户: {}", orderId, currentUserId);

            return Result.success("订单创建成功，订单ID: " + orderId + "，正在处理库存冻结和支付...");

        } catch (Exception e) {
            log.error("❌ 创建单商品订单失败 - 用户: {}, 商品: {}, 错误: {}",
                    currentUserId, orderCreateDTO.getProductId(), e.getMessage(), e);
            return Result.error("订单创建失败: " + e.getMessage());
        }
    }

    /**
     * 查询订单状态
     *
     * @param orderId 订单ID
     * @return 订单状态信息
     */
    @GetMapping("/status/{orderId}")
    @Operation(summary = "查询订单状态", description = "根据订单ID查询当前订单状态和处理进度")
    public Result<String> getOrderStatus(
            @Parameter(description = "订单ID", required = true)
            @PathVariable Long orderId) {

        try {
            String status = simpleOrderService.getOrderStatus(orderId);
            return Result.success(status);

        } catch (Exception e) {
            log.error("❌ 查询订单状态失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return Result.error("查询订单状态失败: " + e.getMessage());
        }
    }

    /**
     * 模拟支付完成
     * 用于测试支付成功后的订单完成流程
     *
     * @param orderId 订单ID
     * @return 支付结果
     */
    @PostMapping("/mock-payment/{orderId}")
    @Operation(summary = "模拟支付完成", description = "模拟支付成功，触发订单完成流程（仅用于测试）")
    public Result<String> mockPaymentComplete(
            @Parameter(description = "订单ID", required = true)
            @PathVariable Long orderId) {

        try {
            log.info("🔄 模拟支付完成 - 订单ID: {}", orderId);

            boolean result = simpleOrderService.mockPaymentComplete(orderId);

            if (result) {
                log.info("✅ 模拟支付完成成功 - 订单ID: {}", orderId);
                return Result.success("支付完成，正在处理订单完成和库存扣减...");
            } else {
                return Result.error("支付处理失败，请检查订单状态");
            }

        } catch (Exception e) {
            log.error("❌ 模拟支付完成失败 - 订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            return Result.error("支付处理失败: " + e.getMessage());
        }
    }
}
