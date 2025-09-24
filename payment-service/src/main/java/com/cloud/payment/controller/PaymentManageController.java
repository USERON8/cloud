package com.cloud.payment.controller;

import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.result.Result;
import com.cloud.payment.converter.PaymentConverter;
import com.cloud.payment.messaging.producer.LogCollectionProducer;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

/**
 * 支付管理控制器
 * 提供支付管理相关接口
 */
@Slf4j
@RestController
@RequestMapping("/payment/manage")
@RequiredArgsConstructor
@Tag(name = "支付类型管理", description = "支付类型管理接口")
public class PaymentManageController {

    private final PaymentService paymentService;
    private final LogCollectionProducer logCollectionProducer;
    private final PaymentConverter paymentConverter = PaymentConverter.INSTANCE;

    /**
     * 创建支付记录
     *
     * @param paymentDTO    支付信息
     * @param currentUserId 当前用户ID
     * @return 创建结果
     */
    @PostMapping
    @Operation(summary = "创建支付记录", description = "创建新的支付记录")
    @CacheEvict(value = {"payment-page"}, allEntries = true)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "支付信息", required = true)
    @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> createPayment(
            @Parameter(description = "支付信息") @Valid @RequestBody PaymentDTO paymentDTO,
            @RequestHeader("X-User-ID") String currentUserId) {
        try {
            log.info("管理员创建支付记录，订单ID: {}，操作人: {}", paymentDTO.getOrderId(), currentUserId);

            Payment payment = paymentConverter.toEntity(paymentDTO);
            boolean saved = paymentService.save(payment);

            if (saved) {
                // 发送支付创建日志
                try {
                    logCollectionProducer.sendPaymentOperationLog(
                            payment.getId(),
                            paymentDTO.getOrderId(),
                            "CREATE",
                            paymentDTO.getAmount(),
                            "管理员创建",
                            currentUserId
                    );
                } catch (Exception e) {
                    log.warn("发送管理员支付创建日志失败，支付ID：{}", payment.getId(), e);
                }

                log.info("创建支付记录成功，订单ID: {}，操作人: {}", paymentDTO.getOrderId(), currentUserId);
                return Result.success("创建成功");
            } else {
                log.error("创建支付记录失败，订单ID: {}，操作人: {}", paymentDTO.getOrderId(), currentUserId);
                return Result.error("创建支付记录失败");
            }
        } catch (Exception e) {
            log.error("创建支付记录异常，订单ID: {}，操作人: {}", paymentDTO.getOrderId(), currentUserId, e);
            return Result.error("创建支付记录失败: " + e.getMessage());
        }
    }

    /**
     * 更新支付记录
     *
     * @param id            支付ID
     * @param paymentDTO    支付信息
     * @param currentUserId 当前用户ID
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新支付记录", description = "更新支付记录信息")
    @CacheEvict(value = {"payment", "payment-page"}, allEntries = true)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "支付信息", required = true)
    @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> updatePayment(
            @Parameter(description = "支付ID") @NotNull @PathVariable Long id,
            @Parameter(description = "支付信息") @Valid @RequestBody PaymentDTO paymentDTO,
            @RequestHeader("X-User-ID") String currentUserId,
            @RequestHeader("X-User-Roles") String currentUserRoles) {
        try {
            log.info("更新支付记录，支付ID: {}，操作人: {}", id, currentUserId);

            Payment existingPayment = paymentService.getById(id);
            if (existingPayment == null) {
                log.warn("支付记录不存在，支付ID: {}，操作人: {}", id, currentUserId);
                return Result.error("支付记录不存在");
            }

            Payment payment = paymentConverter.toEntity(paymentDTO);
            payment.setId(id); // 确保ID一致
            boolean updated = paymentService.updateById(payment);

            if (updated) {
                // 发送支付更新日志
                try {
                    logCollectionProducer.sendPaymentOperationLog(
                            id,
                            paymentDTO.getOrderId(),
                            "UPDATE",
                            paymentDTO.getAmount(),
                            "管理员更新",
                            currentUserId
                    );
                } catch (Exception e) {
                    log.warn("发送管理员支付更新日志失败，支付ID：{}", id, e);
                }

                log.info("更新支付记录成功，支付ID: {}，操作人: {}", id, currentUserId);
                return Result.success("更新成功");
            } else {
                log.error("更新支付记录失败，支付ID: {}，操作人: {}", id, currentUserId);
                return Result.error("更新支付记录失败");
            }
        } catch (Exception e) {
            log.error("更新支付记录异常，支付ID: {}，操作人: {}", id, currentUserId, e);
            return Result.error("更新支付记录失败: " + e.getMessage());
        }
    }

    /**
     * 删除支付记录
     *
     * @param id            支付ID
     * @param currentUserId 当前用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除支付记录", description = "删除支付记录")
    @CacheEvict(value = {"payment", "payment-page"}, allEntries = true)
    @Parameters({
            @Parameter(name = "id", description = "支付ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "删除成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> deletePayment(
            @Parameter(description = "支付ID") @NotNull @PathVariable Long id,
            @RequestHeader("X-User-ID") String currentUserId,
            @RequestHeader("X-User-Roles") String currentUserRoles) {
        try {
            log.info("删除支付记录，支付ID: {}，操作人: {}", id, currentUserId);

            Payment existingPayment = paymentService.getById(id);
            if (existingPayment == null) {
                log.warn("支付记录不存在，支付ID: {}，操作人: {}", id, currentUserId);
                return Result.error("支付记录不存在");
            }

            boolean removed = paymentService.removeById(id);
            if (removed) {
                log.info("删除支付记录成功，支付ID: {}，操作人: {}", id, currentUserId);
                return Result.success("删除成功");
            } else {
                log.error("删除支付记录失败，支付ID: {}，操作人: {}", id, currentUserId);
                return Result.error("删除支付记录失败");
            }
        } catch (Exception e) {
            log.error("删除支付记录异常，支付ID: {}，操作人: {}", id, currentUserId, e);
            return Result.error("删除支付记录失败: " + e.getMessage());
        }
    }
}