package com.cloud.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.result.Result;
import com.cloud.payment.converter.PaymentConverter;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付查询控制器
 * 提供支付查询相关接口
 */
@Slf4j
@RestController
@RequestMapping("/payment/query")
@RequiredArgsConstructor
@Validated
@Tag(name = "支付查询接口", description = "支付查询相关的 RESTful API 接口")
public class PaymentQueryController {

    private final PaymentService paymentService;
    private final PaymentConverter paymentConverter = PaymentConverter.INSTANCE;

    /**
     * 根据ID获取支付记录
     *
     * @param id            支付ID
     * @param currentUserId 当前用户ID
     * @return 支付信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取支付详情", description = "根据ID获取支付详细信息")
    @Cacheable(value = "payment", key = "#id")
    @Parameters({
            @Parameter(name = "id", description = "支付ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<PaymentDTO> getPaymentById(
            @Parameter(description = "支付ID", required = true)
            @PathVariable
            @NotNull(message = "支付ID不能为空")
            @Positive(message = "支付ID必须为正整数") Long id,
            @RequestHeader("X-User-ID") String currentUserId,
            @RequestHeader("X-User-Roles") String currentUserRoles) {

        log.info("获取支付详情，支付ID: {}，操作人: {}", id, currentUserId);
        Payment payment = paymentService.getById(id);

        if (payment == null) {
            log.warn("支付记录不存在，支付ID: {}，操作人: {}", id, currentUserId);
            return Result.error("支付记录不存在");
        }

        PaymentDTO paymentDTO = paymentConverter.toDTO(payment);
        log.info("获取支付详情成功，支付ID: {}，操作人: {}", id, currentUserId);
        return Result.success(paymentDTO);
    }

    /**
     * 分页查询支付记录
     *
     * @param page          页码
     * @param size          每页数量
     * @param userId        用户ID（可选）
     * @param status        支付状态（可选）
     * @param channel       支付渠道（可选）
     * @param currentUserId 当前用户ID
     * @return 支付记录列表
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询支付记录", description = "分页查询支付记录列表")
    @Cacheable(value = "payment-page", key = "'page:' + #page + ':' + #size + ':' + #userId + ':' + #status + ':' + #channel")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Page<PaymentDTO>> getPayments(
            @Parameter(description = "页码")
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于0") Integer page,

            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页数量必须大于0")
            @Max(value = 100, message = "每页数量不能超过100") Integer size,

            @Parameter(description = "用户ID")
            @RequestParam(required = false)
            @Positive(message = "用户ID必须为正整数") Long userId,

            @Parameter(description = "支付状态")
            @RequestParam(required = false)
            @Min(value = 0, message = "支付状态值错误")
            @Max(value = 9, message = "支付状态值错误") Integer status,

            @Parameter(description = "支付渠道")
            @RequestParam(required = false)
            @Min(value = 0, message = "支付渠道值错误")
            @Max(value = 9, message = "支付渠道值错误") Integer channel,

            @RequestHeader("X-User-ID") String currentUserId,
            @RequestHeader("X-User-Roles") String currentUserRoles) {

        log.info("分页查询支付记录，页码: {}，每页数量: {}，用户ID: {}，状态: {}，渠道: {}，操作人: {}",
                page, size, userId, status, channel, currentUserId);

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

        log.info("分页查询支付记录成功，共{}条记录，操作人: {}", dtoPage.getTotal(), currentUserId);
        return Result.success(dtoPage);
    }
}