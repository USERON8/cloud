package com.cloud.admin.controller;

import com.cloud.api.merchant.MerchantFeignClient;
import com.cloud.api.stock.StockFeignClient;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.merchant.MerchantDTO;
import com.cloud.common.domain.dto.merchant.MerchantShopDTO;
import com.cloud.common.enums.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员管理控制器
 * 实现管理员对商家、店铺的审核管理功能
 */
@Slf4j
@RestController
@RequestMapping("/admin/manage")
@RequiredArgsConstructor
@Tag(name = "管理员管理", description = "管理员管理相关接口")
public class AdminManageController {

    private final MerchantFeignClient merchantFeignClient;
    private final StockFeignClient stockFeignClient;

    /**
     * 审核通过商家
     *
     * @param id 商家ID
     * @return 审核结果
     */
    @PutMapping("/merchants/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "审核通过商家", description = "管理员审核通过商家申请")
    @ApiResponse(responseCode = "200", description = "审核成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<MerchantDTO> approveMerchant(@Parameter(description = "商家ID") @PathVariable Long id) {
        log.info("管理员审核通过商家，ID: {}", id);
        try {
            Result<MerchantDTO> result = merchantFeignClient.approveMerchant(id);
            log.info("审核通过商家成功，ID: {}", id);
            return result;
        } catch (Exception e) {
            log.error("审核通过商家失败，ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核商家失败: " + e.getMessage());
        }
    }

    /**
     * 拒绝商家申请
     *
     * @param id 商家ID
     * @return 审核结果
     */
    @PutMapping("/merchants/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "拒绝商家申请", description = "管理员拒绝商家申请")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<MerchantDTO> rejectMerchant(@Parameter(description = "商家ID") @PathVariable Long id) {
        log.info("管理员拒绝商家申请，ID: {}", id);
        try {
            Result<MerchantDTO> result = merchantFeignClient.rejectMerchant(id);
            log.info("拒绝商家申请成功，ID: {}", id);
            return result;
        } catch (Exception e) {
            log.error("拒绝商家申请失败，ID: {}", id, e);
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
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "审核通过店铺", description = "管理员审核通过店铺申请")
    @ApiResponse(responseCode = "200", description = "审核成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<MerchantShopDTO> approveShop(@Parameter(description = "店铺ID") @PathVariable Long id) {
        log.info("管理员审核通过店铺，ID: {}", id);
        try {
            Result<MerchantShopDTO> result = merchantFeignClient.approveShop(id);
            log.info("审核通过店铺成功，ID: {}", id);
            return result;
        } catch (Exception e) {
            log.error("审核通过店铺失败，ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核店铺失败: " + e.getMessage());
        }
    }

    /**
     * 拒绝店铺申请
     *
     * @param id 店铺ID
     * @return 审核结果
     */
    @PutMapping("/shops/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "拒绝店铺申请", description = "管理员拒绝店铺申请")
    @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<MerchantShopDTO> rejectShop(@Parameter(description = "店铺ID") @PathVariable Long id) {
        log.info("管理员拒绝店铺申请，ID: {}", id);
        try {
            Result<MerchantShopDTO> result = merchantFeignClient.rejectShop(id);
            log.info("拒绝店铺申请成功，ID: {}", id);
            return result;
        } catch (Exception e) {
            log.error("拒绝店铺申请失败，ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "拒绝店铺申请失败: " + e.getMessage());
        }
    }

    /**
     * 管理库存
     *
     * @param productId 商品ID
     * @param quantity  库存数量
     * @return 管理结果
     */
    @PutMapping("/stocks/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "更新商品库存", description = "管理员更新指定商品的库存数量")
    @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Void> updateStock(@Parameter(description = "商品ID") @PathVariable Long productId,
                                    @Parameter(description = "库存数量") @RequestParam Integer quantity) {
        log.info("管理员更新商品库存，商品ID: {}，数量: {}", productId, quantity);
        try {
            Result<Void> result = stockFeignClient.updateStock(productId, quantity);
            log.info("更新商品库存成功，商品ID: {}，数量: {}", productId, quantity);
            return result;
        } catch (Exception e) {
            log.error("更新商品库存失败，商品ID: {}，数量: {}", productId, quantity, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新商品库存失败: " + e.getMessage());
        }
    }
}