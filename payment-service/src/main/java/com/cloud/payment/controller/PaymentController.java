package com.cloud.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.payment.converter.PaymentConverter;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付管理控制器
 * 提供支付记录的增删改查及分页查询功能
 */
@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "支付管理", description = "支付管理接口")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentConverter paymentConverter = PaymentConverter.INSTANCE;

    /**
     * 创建支付记录
     *
     * @param paymentDTO 支付信息
     * @return 创建结果
     */
    @PostMapping
    @Operation(summary = "创建支付记录", description = "创建新的支付记录")
    @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<PaymentDTO> createPayment(
            @Parameter(description = "支付信息") @RequestBody PaymentDTO paymentDTO) {
        try {
            log.info("创建支付记录，订单ID: {}", paymentDTO.getOrderId());

            Payment payment = paymentConverter.toEntity(paymentDTO);
            boolean saved = paymentService.save(payment);

            if (saved) {
                Payment savedPayment = paymentService.getById(payment.getId());
                PaymentDTO savedPaymentDTO = paymentConverter.toDTO(savedPayment);
                log.info("创建支付记录成功，支付ID: {}", savedPayment.getId());
                return Result.success(savedPaymentDTO);
            } else {
                log.error("创建支付记录失败，订单ID: {}", paymentDTO.getOrderId());
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建支付记录失败");
            }
        } catch (Exception e) {
            log.error("创建支付记录失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建支付记录失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取支付记录
     *
     * @param id 支付ID
     * @return 支付信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取支付详情", description = "根据ID获取支付详细信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<PaymentDTO> getPaymentById(
            @Parameter(description = "支付ID") @PathVariable Long id) {
        try {
            log.info("获取支付详情，支付ID: {}", id);

            Payment payment = paymentService.getById(id);
            if (payment == null) {
                log.warn("支付记录不存在，支付ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "支付记录不存在");
            }

            PaymentDTO paymentDTO = paymentConverter.toDTO(payment);
            log.info("获取支付详情成功，支付ID: {}", id);
            return Result.success(paymentDTO);
        } catch (Exception e) {
            log.error("获取支付详情失败，支付ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取支付详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新支付记录
     *
     * @param id         支付ID
     * @param paymentDTO 支付信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新支付记录", description = "更新支付记录信息")
    @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<PaymentDTO> updatePayment(
            @Parameter(description = "支付ID") @PathVariable Long id,
            @Parameter(description = "支付信息") @RequestBody PaymentDTO paymentDTO) {
        try {
            log.info("更新支付记录，支付ID: {}", id);

            Payment existingPayment = paymentService.getById(id);
            if (existingPayment == null) {
                log.warn("支付记录不存在，支付ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "支付记录不存在");
            }

            Payment payment = paymentConverter.toEntity(paymentDTO);
            payment.setId(id); // 确保ID一致
            boolean updated = paymentService.updateById(payment);

            if (updated) {
                Payment updatedPayment = paymentService.getById(id);
                PaymentDTO updatedPaymentDTO = paymentConverter.toDTO(updatedPayment);
                log.info("更新支付记录成功，支付ID: {}", id);
                return Result.success(updatedPaymentDTO);
            } else {
                log.error("更新支付记录失败，支付ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新支付记录失败");
            }
        } catch (Exception e) {
            log.error("更新支付记录失败，支付ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新支付记录失败: " + e.getMessage());
        }
    }

    /**
     * 删除支付记录
     *
     * @param id 支付ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除支付记录", description = "删除支付记录")
    @ApiResponse(responseCode = "200", description = "删除成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Void> deletePayment(
            @Parameter(description = "支付ID") @PathVariable Long id) {
        try {
            log.info("删除支付记录，支付ID: {}", id);

            Payment existingPayment = paymentService.getById(id);
            if (existingPayment == null) {
                log.warn("支付记录不存在，支付ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "支付记录不存在");
            }

            boolean removed = paymentService.removeById(id);
            if (removed) {
                log.info("删除支付记录成功，支付ID: {}", id);
                return Result.success();
            } else {
                log.error("删除支付记录失败，支付ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除支付记录失败");
            }
        } catch (Exception e) {
            log.error("删除支付记录失败，支付ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除支付记录失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询支付记录
     *
     * @param page    页码
     * @param size    每页数量
     * @param userId  用户ID（可选）
     * @param status  支付状态（可选）
     * @param channel 支付渠道（可选）
     * @return 支付记录列表
     */
    @GetMapping
    @Operation(summary = "分页查询支付记录", description = "分页查询支付记录列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Page<PaymentDTO>> getPayments(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "支付状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "支付渠道") @RequestParam(required = false) Integer channel) {
        try {
            log.info("分页查询支付记录，页码: {}，每页数量: {}，用户ID: {}，状态: {}，渠道: {}", page, size, userId, status, channel);

            Page<Payment> paymentPage = new Page<>(page, size);
            LambdaQueryWrapper<Payment> queryWrapper = new LambdaQueryWrapper<>();

            // 添加查询条件
            if (userId != null) {
                queryWrapper.eq(Payment::getUserId, userId);
            }
            if (status != null) {
                queryWrapper.eq(Payment::getStatus, status);
            }
            if (channel != null) {
                queryWrapper.eq(Payment::getChannel, channel);
            }

            // 按创建时间倒序排列
            queryWrapper.orderByDesc(Payment::getCreatedAt);

            Page<Payment> resultPage = paymentService.page(paymentPage, queryWrapper);

            // 转换为DTO
            Page<PaymentDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
            List<PaymentDTO> dtoList = paymentConverter.toDTOList(resultPage.getRecords());
            dtoPage.setRecords(dtoList);

            log.info("分页查询支付记录成功，共{}条记录", dtoPage.getTotal());
            return Result.success(dtoPage);
        } catch (Exception e) {
            log.error("分页查询支付记录失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "分页查询支付记录失败: " + e.getMessage());
        }
    }
}