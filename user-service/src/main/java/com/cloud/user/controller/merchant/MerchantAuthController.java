package com.cloud.user.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantAuthRequestDTO;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.converter.MerchantAuthConverter;
import com.cloud.user.module.entity.MerchantAuth;
import com.cloud.user.service.MerchantAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/merchant/auth")
@RequiredArgsConstructor
@Tag(name = "商家认证管理", description = "商家认证申请、查询、撤销等相关操作")
public class MerchantAuthController {
    private final MerchantAuthService merchantAuthService;
    private final MerchantAuthConverter merchantAuthConverter;

    /**
     * 商家申请认证
     *
     * @param merchantId             商家ID
     * @param merchantAuthRequestDTO 认证申请信息
     * @return 认证信息
     */
    @PostMapping("/apply/{merchantId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "商家申请认证", description = "商家提交认证申请")
    public Result<MerchantAuthDTO> applyForAuth(
            @PathVariable("merchantId")
            @Parameter(description = "商家ID")
            @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestBody
            @Parameter(description = "认证申请信息")
            @Valid @NotNull(message = "认证申请信息不能为空") MerchantAuthRequestDTO merchantAuthRequestDTO) {

        // 权限检查：只有商家自己或管理员可以申请认证
        if (!SecurityPermissionUtils.isAdminOrMerchantOwner(null, merchantId)) {
            return Result.forbidden("无权限申请认证");
        }

        log.info("商家申请认证, merchantId: {}", merchantId);

        // 检查是否已存在认证申请
        LambdaQueryWrapper<MerchantAuth> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(MerchantAuth::getMerchantId, merchantId);
        MerchantAuth existingAuth = merchantAuthService.getOne(queryWrapper);

        MerchantAuth merchantAuth;
        if (existingAuth != null) {
            // 更新已有的认证申请
            merchantAuth = merchantAuthConverter.toEntity(merchantAuthRequestDTO);
            merchantAuth.setId(existingAuth.getId());
            merchantAuth.setMerchantId(merchantId);
            merchantAuth.setAuthStatus(0); // 重置为待审核状态
            merchantAuth.setUpdatedAt(LocalDateTime.now());
            merchantAuthService.updateById(merchantAuth);
        } else {
            // 创建新的认证申请
            merchantAuth = merchantAuthConverter.toEntity(merchantAuthRequestDTO);
            merchantAuth.setMerchantId(merchantId);
            merchantAuth.setAuthStatus(0); // 待审核状态
            merchantAuth.setCreatedAt(LocalDateTime.now());
            merchantAuth.setUpdatedAt(LocalDateTime.now());
            merchantAuthService.save(merchantAuth);
        }

        MerchantAuthDTO result = merchantAuthConverter.toDTO(merchantAuth);
        return Result.success("认证申请已提交", result);
    }

    /**
     * 获取商家认证信息
     *
     * @param merchantId 商家ID
     * @return 认证信息
     */
    @GetMapping("/get/{merchantId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取商家认证信息", description = "根据商家ID获取认证信息")
    public Result<MerchantAuthDTO> getAuthInfo(@PathVariable("merchantId")
                                               @Parameter(description = "商家ID")
                                               @NotNull(message = "商家ID不能为空") Long merchantId) {

        // 权限检查：只有商家自己或管理员可以查看认证信息
        if (!SecurityPermissionUtils.isAdminOrMerchantOwner(null, merchantId)) {
            return Result.forbidden("无权限查看认证信息");
        }

        log.info("获取商家认证信息, merchantId: {}", merchantId);

        LambdaQueryWrapper<MerchantAuth> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(MerchantAuth::getMerchantId, merchantId);
        MerchantAuth merchantAuth = merchantAuthService.getOne(queryWrapper);

        if (merchantAuth == null) {
            return Result.success("暂无认证信息", null);
        }

        MerchantAuthDTO result = merchantAuthConverter.toDTO(merchantAuth);
        return Result.success(result);
    }

    /**
     * 撤销认证申请
     *
     * @param merchantId 商家ID
     * @return 操作结果
     */
    @DeleteMapping("/revoke/{merchantId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "撤销认证申请", description = "撤销商家的认证申请")
    public Result<Boolean> revokeAuth(@PathVariable("merchantId")
                                      @Parameter(description = "商家ID")
                                      @NotNull(message = "商家ID不能为空") Long merchantId) {

        // 权限检查：只有商家自己或管理员可以撤销认证申请
        if (!SecurityPermissionUtils.isAdminOrMerchantOwner(null, merchantId)) {
            return Result.forbidden("无权限撤销认证申请");
        }

        log.info("撤销认证申请, merchantId: {}", merchantId);

        LambdaQueryWrapper<MerchantAuth> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(MerchantAuth::getMerchantId, merchantId);
        boolean result = merchantAuthService.remove(queryWrapper);

        if (result) {
            return Result.success("认证申请已撤销", true);
        } else {
            return Result.success("无认证申请可撤销", false);
        }
    }

    /**
     * 管理员审核商家认证申请
     *
     * @param merchantId 商家ID
     * @param authStatus 审核状态（1: 通过，2: 拒绝）
     * @return 操作结果
     */
    @PostMapping("/review/{merchantId}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "管理员审核商家认证", description = "管理员审核商家认证申请")
    public Result<Boolean> reviewAuth(
            @PathVariable("merchantId")
            @Parameter(description = "商家ID")
            @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam("authStatus")
            @Parameter(description = "审核状态")
            @NotNull(message = "审核状态不能为空") Integer authStatus) {


        log.info("管理员审核商家认证, merchantId: {}, authStatus: {}", merchantId, authStatus);

        LambdaQueryWrapper<MerchantAuth> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(MerchantAuth::getMerchantId, merchantId);
        MerchantAuth merchantAuth = merchantAuthService.getOne(queryWrapper);

        if (merchantAuth == null) {
            return Result.error("未找到商家认证信息");
        }

        merchantAuth.setAuthStatus(authStatus);
        merchantAuth.setUpdatedAt(LocalDateTime.now());
        boolean result = merchantAuthService.updateById(merchantAuth);

        if (result) {
            String statusDesc = authStatus == 1 ? "审核通过" : "审核拒绝";
            return Result.success("商家认证已" + statusDesc, true);
        } else {
            return Result.error("审核失败");
        }
    }

    /**
     * 根据认证状态查询商家认证信息
     *
     * @param authStatus 认证状态
     * @return 商家认证信息列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    @Operation(summary = "根据状态查询商家认证", description = "根据认证状态查询所有商家认证信息")
    public Result<java.util.List<MerchantAuthDTO>> listAuthByStatus(
            @RequestParam("authStatus")
            @Parameter(description = "认证状态")
            @NotNull(message = "认证状态不能为空") Integer authStatus) {


        log.info("根据认证状态查询商家信息, authStatus: {}", authStatus);

        LambdaQueryWrapper<MerchantAuth> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(MerchantAuth::getAuthStatus, authStatus);
        java.util.List<MerchantAuth> merchantAuthList = merchantAuthService.list(queryWrapper);

        java.util.List<MerchantAuthDTO> result = merchantAuthList.stream()
                .map(merchantAuthConverter::toDTO)
                .toList();

        return Result.success(result);
    }

    /**
     * 批量审核商家认证申请
     *
     * @param merchantIds 商家ID列表
     * @param authStatus  审核状态（1: 通过，2: 拒绝）
     * @return 操作结果
     */
    @PostMapping("/review/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "批量审核商家认证", description = "批量审核商家认证申请")
    public Result<Boolean> reviewAuthBatch(
            @RequestBody
            @Parameter(description = "商家ID列表")
            @NotNull(message = "商家ID列表不能为空") java.util.List<Long> merchantIds,
            @RequestParam("authStatus")
            @Parameter(description = "审核状态")
            @NotNull(message = "审核状态不能为空") Integer authStatus) {

        if (merchantIds == null || merchantIds.isEmpty()) {
            return Result.badRequest("商家ID列表不能为空");
        }

        if (merchantIds.size() > 100) {
            return Result.badRequest("批量审核数量不能超过100个");
        }

        log.info("批量审核商家认证, merchantIds: {}, authStatus: {}", merchantIds, authStatus);

        int successCount = 0;
        for (Long merchantId : merchantIds) {
            try {
                LambdaQueryWrapper<MerchantAuth> queryWrapper = Wrappers.lambdaQuery();
                queryWrapper.eq(MerchantAuth::getMerchantId, merchantId);
                MerchantAuth merchantAuth = merchantAuthService.getOne(queryWrapper);

                if (merchantAuth != null) {
                    merchantAuth.setAuthStatus(authStatus);
                    merchantAuth.setUpdatedAt(LocalDateTime.now());
                    if (merchantAuthService.updateById(merchantAuth)) {
                        successCount++;
                    }
                }
            } catch (Exception e) {
                log.error("审核商家认证失败, merchantId: {}", merchantId, e);
            }
        }

        String statusDesc = authStatus == 1 ? "审核通过" : "审核拒绝";
        log.info("批量审核商家认证完成, 成功: {}/{}", successCount, merchantIds.size());
        return Result.success(String.format("批量%s成功: %d/%d", statusDesc, successCount, merchantIds.size()), true);
    }

}
