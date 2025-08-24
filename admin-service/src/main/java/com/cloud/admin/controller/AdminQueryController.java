package com.cloud.admin.controller;

import com.cloud.admin.module.entity.Admin;
import com.cloud.admin.service.AdminService;
import com.cloud.api.merchant.MerchantFeignClient;
import com.cloud.api.stock.StockFeignClient;
import com.cloud.api.user.UserFeignClient;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.merchant.MerchantDTO;
import com.cloud.common.domain.dto.merchant.MerchantShopDTO;
import com.cloud.common.domain.dto.user.UserDTO;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员查询控制器
 * 实现管理员对商家、用户、店铺的查询功能
 */
@Slf4j
@RestController
@RequestMapping("/admin/query")
@RequiredArgsConstructor
@Tag(name = "管理员查询", description = "管理员查询相关接口")
public class AdminQueryController {

    private final UserFeignClient userFeignClient;
    private final MerchantFeignClient merchantFeignClient;
    private final StockFeignClient stockFeignClient;
    private final AdminService adminService;

    /**
     * 获取所有用户列表
     *
     * @return 用户列表
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "获取所有用户", description = "管理员获取所有用户列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<List<UserDTO>> getAllUsers() {
        log.info("管理员获取所有用户列表");
        try {
            Result<List<UserDTO>> result = userFeignClient.getAllUsers();
            log.info("获取所有用户列表成功，共{}条记录", result.getData().size());
            return result;
        } catch (Exception e) {
            log.error("获取所有用户列表失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有商家列表
     *
     * @return 商家列表
     */
    @GetMapping("/merchants")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "获取所有商家", description = "管理员获取所有商家列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<List<MerchantDTO>> getAllMerchants() {
        log.info("管理员获取所有商家列表");
        try {
            Result<List<MerchantDTO>> result = merchantFeignClient.getAllMerchants();
            log.info("获取所有商家列表成功，共{}条记录", result.getData().size());
            return result;
        } catch (Exception e) {
            log.error("获取所有商家列表失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取商家列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有店铺列表
     *
     * @return 店铺列表
     */
    @GetMapping("/shops")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "获取所有店铺", description = "管理员获取所有店铺列表")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<List<MerchantShopDTO>> getAllShops() {
        log.info("管理员获取所有店铺列表");
        try {
            Result<List<MerchantShopDTO>> result = merchantFeignClient.getAllShops();
            log.info("获取所有店铺列表成功，共{}条记录", result.getData().size());
            return result;
        } catch (Exception e) {
            log.error("获取所有店铺列表失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取店铺列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询库存
     *
     * @param productId 商品ID
     * @return 库存信息
     */
    @GetMapping("/stocks/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "查询商品库存", description = "管理员查询指定商品的库存")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Object> getStockByProductId(@Parameter(description = "商品ID") @PathVariable Long productId) {
        log.info("管理员查询商品库存，商品ID: {}", productId);
        try {
            Result<Object> result = stockFeignClient.getStock(productId);
            log.info("查询商品库存成功，商品ID: {}", productId);
            return result;
        } catch (Exception e) {
            log.error("查询商品库存失败，商品ID: {}", productId, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "查询商品库存失败: " + e.getMessage());
        }
    }

    /**
     * 获取管理员信息
     *
     * @param adminId 管理员ID
     * @return 管理员信息
     */
    @GetMapping("/info/{adminId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "获取管理员信息", description = "根据ID获取管理员信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Admin> getAdminInfo(
            @Parameter(description = "管理员ID") @PathVariable Long adminId) {
        log.info("获取管理员信息，管理员ID: {}", adminId);

        Admin admin = adminService.getById(adminId);
        if (admin == null) {
            log.warn("管理员不存在，管理员ID: {}", adminId);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "管理员不存在");
        }

        log.info("获取管理员信息成功，管理员ID: {}", adminId);
        return Result.success(admin);
    }
}