package com.cloud.merchant.controller;

import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.merchant.MerchantDTO;
import com.cloud.common.domain.dto.merchant.MerchantShopDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.merchant.converter.MerchantConverter;
import com.cloud.merchant.module.entity.Merchant;
import com.cloud.merchant.module.entity.MerchantShop;
import com.cloud.merchant.service.MerchantService;
import com.cloud.merchant.service.MerchantShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 商家管理控制器（管理员专用）
 * 处理管理员对商家和店铺的审核操作
 */
@Slf4j
@RestController
@RequestMapping("/admin/merchants")
@RequiredArgsConstructor
@Tag(name = "商家管理(管理员)", description = "管理员对商家和店铺的审核接口")
public class MerchantAdminController {

    private final MerchantService merchantService;
    private final MerchantShopService merchantShopService;
    private final MerchantConverter merchantConverter = MerchantConverter.INSTANCE;

    /**
     * 审核通过商家
     *
     * @param id 商家ID
     * @return 审核结果
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "审核通过商家", description = "管理员审核通过商家申请")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<MerchantDTO> approveMerchant(
            @Parameter(description = "商家ID") @PathVariable Long id) {
        try {
            log.info("管理员审核通过商家，商家ID: {}", id);

            Merchant merchant = merchantService.getById(id);
            if (merchant == null) {
                log.warn("商家不存在，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商家不存在");
            }

            // 设置商家状态为启用(1)
            merchant.setStatus(1);
            boolean updated = merchantService.updateById(merchant);

            if (updated) {
                MerchantDTO merchantDTO = merchantConverter.toDTO(merchant);
                log.info("审核通过商家成功，商家ID: {}", id);
                return Result.success(merchantDTO);
            } else {
                log.error("审核通过商家失败，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核通过商家失败");
            }
        } catch (Exception e) {
            log.error("审核通过商家失败，商家ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核通过商家失败: " + e.getMessage());
        }
    }

    /**
     * 拒绝商家申请
     *
     * @param id 商家ID
     * @return 审核结果
     */
    @PutMapping("/{id}/reject")
    @Operation(summary = "拒绝商家申请", description = "管理员拒绝商家申请")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<MerchantDTO> rejectMerchant(
            @Parameter(description = "商家ID") @PathVariable Long id) {
        try {
            log.info("管理员拒绝商家申请，商家ID: {}", id);

            Merchant merchant = merchantService.getById(id);
            if (merchant == null) {
                log.warn("商家不存在，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商家不存在");
            }

            // 设置商家状态为禁用(0)
            merchant.setStatus(0);
            boolean updated = merchantService.updateById(merchant);

            if (updated) {
                MerchantDTO merchantDTO = merchantConverter.toDTO(merchant);
                log.info("拒绝商家申请成功，商家ID: {}", id);
                return Result.success(merchantDTO);
            } else {
                log.error("拒绝商家申请失败，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "拒绝商家申请失败");
            }
        } catch (Exception e) {
            log.error("拒绝商家申请失败，商家ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "拒绝商家申请失败: " + e.getMessage());
        }
    }

    /**
     * 审核通过店铺
     *
     * @param id 店铺ID
     * @return 审核结果
     */
    @PutMapping("/shops/{id}/approve")
    @Operation(summary = "审核通过店铺", description = "管理员审核通过店铺申请")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<MerchantShopDTO> approveShop(
            @Parameter(description = "店铺ID") @PathVariable Long id) {
        try {
            log.info("管理员审核通过店铺，店铺ID: {}", id);

            MerchantShop shop = merchantShopService.getById(id);
            if (shop == null) {
                log.warn("店铺不存在，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "店铺不存在");
            }

            // 设置店铺状态为营业(1)
            shop.setStatus(1);
            boolean updated = merchantShopService.updateById(shop);

            if (updated) {
                MerchantShopDTO shopDTO = merchantConverter.toShopDTO(shop);
                log.info("审核通过店铺成功，店铺ID: {}", id);
                return Result.success(shopDTO);
            } else {
                log.error("审核通过店铺失败，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核通过店铺失败");
            }
        } catch (Exception e) {
            log.error("审核通过店铺失败，店铺ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核通过店铺失败: " + e.getMessage());
        }
    }

    /**
     * 拒绝店铺申请
     *
     * @param id 店铺ID
     * @return 审核结果
     */
    @PutMapping("/shops/{id}/reject")
    @Operation(summary = "拒绝店铺申请", description = "管理员拒绝店铺申请")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<MerchantShopDTO> rejectShop(
            @Parameter(description = "店铺ID") @PathVariable Long id) {
        try {
            log.info("管理员拒绝店铺申请，店铺ID: {}", id);

            MerchantShop shop = merchantShopService.getById(id);
            if (shop == null) {
                log.warn("店铺不存在，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "店铺不存在");
            }

            // 设置店铺状态为关闭(0)
            shop.setStatus(0);
            boolean updated = merchantShopService.updateById(shop);

            if (updated) {
                MerchantShopDTO shopDTO = merchantConverter.toShopDTO(shop);
                log.info("拒绝店铺申请成功，店铺ID: {}", id);
                return Result.success(shopDTO);
            } else {
                log.error("拒绝店铺申请失败，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "拒绝店铺申请失败");
            }
        } catch (Exception e) {
            log.error("拒绝店铺申请失败，店铺ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "拒绝店铺申请失败: " + e.getMessage());
        }
    }
}