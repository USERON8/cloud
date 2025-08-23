package com.cloud.merchant.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.Result;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.domain.dto.merchant.MerchantShopDTO;
import com.cloud.merchant.converter.MerchantConverter;
import com.cloud.merchant.module.entity.MerchantShop;
import com.cloud.merchant.service.MerchantShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家店铺控制器
 * 处理商家店铺的增删改查操作，只允许商家操作自己的店铺
 */
@Slf4j
@RestController
@RequestMapping("/shops")
@RequiredArgsConstructor
@Tag(name = "商家店铺", description = "商家店铺管理接口")
public class ShopController {

    private final MerchantShopService merchantShopService;
    private final MerchantConverter merchantConverter = MerchantConverter.INSTANCE;

    /**
     * 创建店铺
     * 只允许商家创建属于自己的店铺
     *
     * @param shopDTO        店铺信息
     * @param authentication 认证信息
     * @return 创建结果
     */
    @PostMapping
    @Operation(summary = "创建店铺", description = "创建新的商家店铺")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    public Result<MerchantShopDTO> createShop(
            @Parameter(description = "店铺信息") @RequestBody MerchantShopDTO shopDTO,
            Authentication authentication) {
        try {
            log.info("创建店铺，店铺名称: {}", shopDTO.getShopName());

            Long merchantId = getCurrentMerchantId(authentication);

            // 转换DTO为实体
            MerchantShop shop = merchantConverter.toShopEntity(shopDTO);

            // 设置商家ID为当前认证商家
            shop.setMerchantId(merchantId);

            // 保存店铺信息
            boolean saved = merchantShopService.save(shop);
            if (saved) {
                MerchantShopDTO savedShopDTO = merchantConverter.toShopDTO(shop);
                log.info("创建店铺成功，店铺ID: {}", shop.getId());
                return Result.success(savedShopDTO);
            } else {
                log.error("创建店铺失败");
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建店铺失败");
            }
        } catch (Exception e) {
            log.error("创建店铺失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建店铺失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前商家的所有店铺
     * 只允许商家获取自己的店铺
     *
     * @param authentication 认证信息
     * @return 店铺列表
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前商家的所有店铺", description = "获取当前认证商家的所有店铺")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    public Result<List<MerchantShopDTO>> getCurrentMerchantShops(Authentication authentication) {
        try {
            log.info("获取当前商家的所有店铺");

            Long merchantId = getCurrentMerchantId(authentication);

            // 查询当前商家的所有店铺
            LambdaQueryWrapper<MerchantShop> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MerchantShop::getMerchantId, merchantId);
            List<MerchantShop> shops = merchantShopService.list(queryWrapper);
            List<MerchantShopDTO> shopDTOs = merchantConverter.toShopDTOList(shops);

            log.info("获取当前商家店铺成功，共{}条记录", shopDTOs.size());
            return Result.success(shopDTOs);
        } catch (Exception e) {
            log.error("获取当前商家店铺失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取店铺列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据商家ID获取店铺列表（管理员专用）
     *
     * @param merchantId 商家ID
     * @return 店铺列表
     */
    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "获取商家的所有店铺", description = "根据商家ID获取其所有店铺（仅管理员可访问）")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<List<MerchantShopDTO>> getShopsByMerchantId(
            @Parameter(description = "商家ID") @PathVariable Long merchantId) {
        try {
            log.info("获取商家的所有店铺，商家ID: {}", merchantId);

            // 查询指定商家的所有店铺
            LambdaQueryWrapper<MerchantShop> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MerchantShop::getMerchantId, merchantId);
            List<MerchantShop> shops = merchantShopService.list(queryWrapper);
            List<MerchantShopDTO> shopDTOs = merchantConverter.toShopDTOList(shops);

            log.info("获取商家店铺成功，商家ID: {}，共{}条记录", merchantId, shopDTOs.size());
            return Result.success(shopDTOs);
        } catch (Exception e) {
            log.error("获取商家店铺失败，商家ID: {}", merchantId, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取店铺列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取店铺详情
     * 只允许商家获取自己店铺的详情
     *
     * @param id             店铺ID
     * @param authentication 认证信息
     * @return 店铺详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取店铺详情", description = "根据ID获取店铺详情")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    public Result<MerchantShopDTO> getShopById(
            @Parameter(description = "店铺ID") @PathVariable Long id,
            Authentication authentication) {
        try {
            log.info("获取店铺详情，店铺ID: {}", id);

            Long merchantId = getCurrentMerchantId(authentication);

            // 查询店铺并验证是否属于当前商家
            MerchantShop shop = merchantShopService.getById(id);
            if (shop == null) {
                log.warn("店铺不存在，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "店铺不存在");
            }

            // 验证店铺是否属于当前商家
            if (!shop.getMerchantId().equals(merchantId)) {
                log.warn("无权限访问该店铺，店铺ID: {}，商家ID: {}", id, merchantId);
                return Result.error(ResultCode.FORBIDDEN.getCode(), "无权限访问该店铺");
            }

            MerchantShopDTO shopDTO = merchantConverter.toShopDTO(shop);
            log.info("获取店铺详情成功，店铺ID: {}", id);
            return Result.success(shopDTO);
        } catch (Exception e) {
            log.error("获取店铺详情失败，店铺ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取店铺详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新店铺信息
     * 只允许商家更新自己店铺的信息
     *
     * @param id             店铺ID
     * @param shopDTO        店铺信息
     * @param authentication 认证信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新店铺信息", description = "更新指定店铺的信息")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    public Result<MerchantShopDTO> updateShop(
            @Parameter(description = "店铺ID") @PathVariable Long id,
            @Parameter(description = "店铺信息") @RequestBody MerchantShopDTO shopDTO,
            Authentication authentication) {
        try {
            log.info("更新店铺信息，店铺ID: {}", id);

            Long merchantId = getCurrentMerchantId(authentication);

            // 查询店铺并验证是否属于当前商家
            MerchantShop existingShop = merchantShopService.getById(id);
            if (existingShop == null) {
                log.warn("店铺不存在，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "店铺不存在");
            }

            // 验证店铺是否属于当前商家
            if (!existingShop.getMerchantId().equals(merchantId)) {
                log.warn("无权限更新该店铺，店铺ID: {}，商家ID: {}", id, merchantId);
                return Result.error(ResultCode.FORBIDDEN.getCode(), "无权限更新该店铺");
            }

            // 转换DTO为实体
            MerchantShop shop = merchantConverter.toShopEntity(shopDTO);

            // 设置店铺ID和商家ID
            shop.setId(id);
            shop.setMerchantId(merchantId);

            // 更新店铺信息
            boolean updated = merchantShopService.updateById(shop);
            if (updated) {
                MerchantShop updatedShop = merchantShopService.getById(id);
                MerchantShopDTO updatedShopDTO = merchantConverter.toShopDTO(updatedShop);
                log.info("更新店铺信息成功，店铺ID: {}", id);
                return Result.success(updatedShopDTO);
            } else {
                log.error("更新店铺信息失败，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新店铺信息失败");
            }
        } catch (Exception e) {
            log.error("更新店铺信息失败，店铺ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新店铺信息失败: " + e.getMessage());
        }
    }

    /**
     * 删除店铺
     * 只允许商家删除自己的店铺
     *
     * @param id             店铺ID
     * @param authentication 认证信息
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除店铺", description = "删除指定的店铺")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    public Result<Void> deleteShop(
            @Parameter(description = "店铺ID") @PathVariable Long id,
            Authentication authentication) {
        try {
            log.info("删除店铺，店铺ID: {}", id);

            Long merchantId = getCurrentMerchantId(authentication);

            // 查询店铺并验证是否属于当前商家
            MerchantShop shop = merchantShopService.getById(id);
            if (shop == null) {
                log.warn("店铺不存在，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "店铺不存在");
            }

            // 验证店铺是否属于当前商家
            if (!shop.getMerchantId().equals(merchantId)) {
                log.warn("无权限删除该店铺，店铺ID: {}，商家ID: {}", id, merchantId);
                return Result.error(ResultCode.FORBIDDEN.getCode(), "无权限删除该店铺");
            }

            // 删除店铺
            boolean removed = merchantShopService.removeById(id);
            if (removed) {
                log.info("删除店铺成功，店铺ID: {}", id);
                return Result.success(null);
            } else {
                log.error("删除店铺失败，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除店铺失败");
            }
        } catch (Exception e) {
            log.error("删除店铺失败，店铺ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除店铺失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有店铺（管理员专用）
     *
     * @return 店铺列表
     */
    @GetMapping
    @Operation(summary = "获取所有店铺", description = "获取所有店铺信息（仅管理员可访问）")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<List<MerchantShopDTO>> getAllShops() {
        try {
            log.info("获取所有店铺");

            List<MerchantShop> shops = merchantShopService.list();
            List<MerchantShopDTO> shopDTOs = merchantConverter.toShopDTOList(shops);

            log.info("获取所有店铺成功，共{}条记录", shopDTOs.size());
            return Result.success(shopDTOs);
        } catch (Exception e) {
            log.error("获取所有店铺失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取店铺列表失败: " + e.getMessage());
        }
    }

    /**
     * 从认证信息中获取当前商家ID
     *
     * @param authentication 认证信息
     * @return 商家ID
     */
    private Long getCurrentMerchantId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String subject = jwtAuth.getToken().getSubject();
            return Long.valueOf(subject);
        }
        throw new IllegalArgumentException("无法从认证信息中获取商家ID");
    }
}