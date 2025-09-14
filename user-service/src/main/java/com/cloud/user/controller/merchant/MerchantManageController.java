package com.cloud.user.controller.merchant;

import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.domain.dto.user.MerchantShopDTO;
import com.cloud.common.result.Result;
import com.cloud.user.constants.OAuth2Permissions;
import com.cloud.user.converter.MerchantAuthConverter;
import com.cloud.user.converter.MerchantConverter;
import com.cloud.user.module.entity.Merchant;
import com.cloud.user.module.entity.MerchantAuth;
import com.cloud.user.security.UserPermissionHelper;
import com.cloud.user.service.MerchantAuthService;
import com.cloud.user.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/merchant/manage")
@RequiredArgsConstructor
@Tag(name = "商家管理", description = "商家审核、店铺管理等相关操作")
public class MerchantManageController {

    private final MerchantService merchantService;
    private final MerchantAuthService merchantAuthService;
    private final UserPermissionHelper permissionHelper;

    private final MerchantConverter merchantConverter = MerchantConverter.INSTANCE;
    private final MerchantAuthConverter merchantAuthConverter = MerchantAuthConverter.INSTANCE;

    @PutMapping("/approveMerchant/{id}")
    @Operation(summary = "审核通过商家", description = "审核通过商家的申请")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<MerchantDTO> approveMerchant(@PathVariable("id")
                                               @Parameter(description = "商家ID")
                                               @NotNull(message = "商家ID不能为空") Long id,
                                               Authentication authentication) {


        Merchant merchant = merchantService.getById(id);
        if (merchant == null) {
            return Result.error("商家不存在");
        }
        merchant.setStatus(1); // 设置为启用状态
        merchant.setUpdatedAt(LocalDateTime.now());
        boolean updated = merchantService.updateById(merchant);
        if (updated) {
            MerchantDTO merchantDTO = merchantConverter.toDTO(merchant);
            return Result.success("审核通过", merchantDTO);
        } else {
            return Result.error("审核失败");
        }
    }

    @PutMapping("/rejectMerchant/{id}")
    @Operation(summary = "拒绝商家申请", description = "拒绝商家的申请")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<MerchantDTO> rejectMerchant(@PathVariable("id")
                                              @Parameter(description = "商家ID")
                                              @NotNull(message = "商家ID不能为空") Long id,
                                              Authentication authentication) {


        Merchant merchant = merchantService.getById(id);
        if (merchant == null) {
            return Result.error("商家不存在");
        }
        merchant.setStatus(0); // 设置为禁用状态
        merchant.setUpdatedAt(LocalDateTime.now());
        boolean updated = merchantService.updateById(merchant);
        if (updated) {
            MerchantDTO merchantDTO = merchantConverter.toDTO(merchant);
            return Result.success("拒绝申请", merchantDTO);
        } else {
            return Result.error("操作失败");
        }
    }

    @PutMapping("/updateMerchant/{id}")
    @Operation(summary = "更新商家信息", description = "更新商家的信息")
    public Result<MerchantDTO> updateMerchant(@PathVariable("id")
                                              @Parameter(description = "商家ID")
                                              @NotNull(message = "商家ID不能为空") Long id,
                                              @RequestBody
                                              @Parameter(description = "商家信息")
                                              @Valid @NotNull(message = "商家信息不能为空") MerchantDTO merchantDTO,
                                              Authentication authentication) {

        // 权限检查：只有管理员或商家自己可以更新商家信息
        if (!(permissionHelper.isMerchantOwner(authentication, id) || permissionHelper.isAdmin(authentication))) {
            return Result.error("无权执行此操作");
        }

        merchantDTO.setId(id);
        Merchant merchant = merchantConverter.toEntity(merchantDTO);
        merchant.setUpdatedAt(LocalDateTime.now());
        boolean updated = merchantService.updateById(merchant);
        if (updated) {
            Merchant updatedMerchant = merchantService.getById(id);
            MerchantDTO updatedMerchantDTO = merchantConverter.toDTO(updatedMerchant);
            return Result.success("更新成功", updatedMerchantDTO);
        } else {
            return Result.error("更新失败");
        }
    }

    @PostMapping("/createShop")
    @Operation(summary = "创建店铺", description = "为商家创建店铺")
    public Result<MerchantShopDTO> createShop(@RequestBody
                                              @Parameter(description = "店铺信息")
                                              @Valid @NotNull(message = "店铺信息不能为空") MerchantShopDTO shop,
                                              Authentication authentication) {

        // 权限检查：只有管理员或商家自己可以创建店铺
        if (!(permissionHelper.isMerchantOwner(authentication, shop.getMerchantId()) || permissionHelper.isAdmin(authentication))) {
            return Result.error("无权执行此操作");
        }

        // TODO: 实现店铺创建逻辑
        return Result.success("创建成功", shop);
    }

    @PutMapping("/updateShop/{id}")
    @Operation(summary = "更新店铺信息", description = "更新店铺的信息")
    public Result<MerchantShopDTO> updateShop(@PathVariable("id")
                                              @Parameter(description = "店铺ID")
                                              @NotNull(message = "店铺ID不能为空") Long id,
                                              @RequestBody
                                              @Parameter(description = "店铺信息")
                                              @Valid @NotNull(message = "店铺信息不能为空") MerchantShopDTO shop,
                                              Authentication authentication) {

        // 权限检查：只有管理员或店铺所有者可以更新店铺信息
        // 注意：这里需要验证店铺是否属于当前商家，但简化处理，仅验证商家ID
        if (!(permissionHelper.isMerchantOwner(authentication, shop.getMerchantId()) || permissionHelper.isAdmin(authentication))) {
            return Result.error("无权执行此操作");
        }

        // TODO: 实现店铺更新逻辑
        return Result.success("更新成功", shop);
    }

    @PutMapping("/approveShop/{id}")
    @Operation(summary = "审核通过店铺", description = "审核通过店铺的申请")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<MerchantShopDTO> approveShop(@PathVariable("id")
                                               @Parameter(description = "店铺ID")
                                               @NotNull(message = "店铺ID不能为空") Long id,
                                               Authentication authentication) {


        // TODO: 实现店铺审核通过逻辑
        return Result.success("审核通过", new MerchantShopDTO());
    }

    @PutMapping("/rejectShop/{id}")
    @Operation(summary = "拒绝店铺申请", description = "拒绝店铺的申请")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<MerchantShopDTO> rejectShop(@PathVariable("id")
                                              @Parameter(description = "店铺ID")
                                              @NotNull(message = "店铺ID不能为空") Long id,
                                              Authentication authentication) {


        // TODO: 实现店铺拒绝逻辑
        return Result.success("拒绝申请", new MerchantShopDTO());
    }

    @DeleteMapping("/deleteShop/{id}")
    @Operation(summary = "删除店铺", description = "删除指定的店铺")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<Boolean> deleteShop(@PathVariable("id")
                                      @Parameter(description = "店铺ID")
                                      @NotNull(message = "店铺ID不能为空") Long id,
                                      Authentication authentication) {


        // TODO: 实现店铺删除逻辑
        return Result.success("删除成功", true);
    }

    @PutMapping("/reviewMerchantAuth/{id}")
    @Operation(summary = "审核商家认证", description = "审核商家的认证申请")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<MerchantAuthDTO> reviewMerchantAuth(@PathVariable("id")
                                                      @Parameter(description = "认证ID")
                                                      @NotNull(message = "认证ID不能为空") Long id,
                                                      @RequestBody
                                                      @Parameter(description = "认证信息")
                                                      @Valid @NotNull(message = "认证信息不能为空") MerchantAuthDTO authDTO,
                                                      Authentication authentication) {


        MerchantAuth merchantAuth = merchantAuthService.getById(id);
        if (merchantAuth == null) {
            return Result.error("认证信息不存在");
        }

        merchantAuth.setAuthStatus(authDTO.getAuthStatus());
        merchantAuth.setAuthRemark(authDTO.getAuthRemark());
        merchantAuth.setUpdatedAt(LocalDateTime.now());
        boolean updated = merchantAuthService.updateById(merchantAuth);
        if (updated) {
            MerchantAuth updatedAuth = merchantAuthService.getById(id);
            MerchantAuthDTO updatedAuthDTO = merchantAuthConverter.toDTO(updatedAuth);
            return Result.success("审核完成", updatedAuthDTO);
        } else {
            return Result.error("审核失败");
        }
    }

}