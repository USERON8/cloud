package com.cloud.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.user.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 商家RESTful API控制器
 * 提供商家资源的CRUD操作
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
@Tag(name = "商家服务", description = "商家资源的RESTful API接口")
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 分页查询商家
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
    @Operation(summary = "分页查询商家", description = "获取商家列表，支持分页")
    public Result<PageResult<MerchantDTO>> getMerchants(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于0") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页数量必须大于0")
            @Max(value = 100, message = "每页数量不能超过100") Integer size,
            @Parameter(description = "审核状态") @RequestParam(required = false) Integer status,
            Authentication authentication) {

        try {
            Page<MerchantDTO> pageResult = merchantService.getMerchantsPage(page, size, status);
            PageResult<MerchantDTO> result = PageResult.of(
                    pageResult.getCurrent(),
                    pageResult.getSize(),
                    pageResult.getTotal(),
                    pageResult.getRecords()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询商家失败", e);
            return Result.error("分页查询商家失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取商家详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionManager.isMerchantOwner(#id, authentication)")
    @Operation(summary = "获取商家详情", description = "根据商家ID获取详细信息")
    public Result<MerchantDTO> getMerchantById(
            @Parameter(description = "商家ID") @PathVariable
            @NotNull(message = "商家ID不能为空")
            @Positive(message = "商家ID必须为正整数") Long id,
            Authentication authentication) {

        try {
            MerchantDTO merchant = merchantService.getMerchantById(id);
            if (merchant == null) {
                return Result.error("商家不存在");
            }
            return Result.success("查询成功", merchant);
        } catch (Exception e) {
            log.error("获取商家详情失败，商家ID: {}", id, e);
            return Result.error("获取商家详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建商家（商家入驻申请）
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建商家", description = "商家入驻申请")
    public Result<MerchantDTO> createMerchant(
            @Parameter(description = "商家信息") @RequestBody
            @Valid @NotNull(message = "商家信息不能为空") MerchantDTO merchantDTO,
            Authentication authentication) {

        try {
            MerchantDTO created = merchantService.createMerchant(merchantDTO);
            return Result.success("商家入驻申请提交成功", created);
        } catch (Exception e) {
            log.error("创建商家失败", e);
            return Result.error("创建商家失败: " + e.getMessage());
        }
    }

    /**
     * 更新商家信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @permissionManager.isMerchantOwner(#id, authentication)")
    @Operation(summary = "更新商家信息", description = "更新商家信息")
    public Result<Boolean> updateMerchant(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Parameter(description = "商家信息") @RequestBody
            @Valid @NotNull(message = "商家信息不能为空") MerchantDTO merchantDTO,
            Authentication authentication) {

        merchantDTO.setId(id);

        try {
            boolean result = merchantService.updateMerchant(merchantDTO);
            return Result.success("商家更新成功", result);
        } catch (Exception e) {
            log.error("更新商家失败，商家ID: {}", id, e);
            return Result.error("更新商家失败: " + e.getMessage());
        }
    }

    /**
     * 删除商家
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "删除商家", description = "删除商家")
    public Result<Boolean> deleteMerchant(
            @Parameter(description = "商家ID") @PathVariable
            @NotNull(message = "商家ID不能为空") Long id) {

        try {
            boolean result = merchantService.deleteMerchant(id);
            return Result.success("删除成功", result);
        } catch (Exception e) {
            log.error("删除商家失败，商家ID: {}", id, e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 审核商家（通过）
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "审核通过商家", description = "审核通过商家的入驻申请")
    public Result<Boolean> approveMerchant(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Parameter(description = "审核备注") @RequestParam(required = false) String remark) {

        try {
            boolean result = merchantService.approveMerchant(id, remark);
            return Result.success("审核通过", result);
        } catch (Exception e) {
            log.error("审核商家失败，商家ID: {}", id, e);
            return Result.error("审核失败: " + e.getMessage());
        }
    }

    /**
     * 审核商家（拒绝）
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "拒绝商家申请", description = "拒绝商家的入驻申请")
    public Result<Boolean> rejectMerchant(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam String reason) {

        try {
            boolean result = merchantService.rejectMerchant(id, reason);
            return Result.success("已拒绝申请", result);
        } catch (Exception e) {
            log.error("拒绝商家申请失败，商家ID: {}", id, e);
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 更新商家状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "更新商家状态", description = "启用或禁用商家")
    public Result<Boolean> updateMerchantStatus(
            @Parameter(description = "商家ID") @PathVariable Long id,
            @Parameter(description = "商家状态") @RequestParam Integer status) {

        try {
            boolean result = merchantService.updateMerchantStatus(id, status);
            return Result.success("状态更新成功", result);
        } catch (Exception e) {
            log.error("更新商家状态失败，商家ID: {}, 状态: {}", id, status, e);
            return Result.error("更新状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取商家统计信息
     */
    @GetMapping("/{id}/statistics")
    @PreAuthorize("hasRole('ADMIN') or @permissionManager.isMerchantOwner(#id, authentication)")
    @Operation(summary = "获取商家统计信息", description = "获取商家的业务统计数据")
    public Result<?> getMerchantStatistics(
            @Parameter(description = "商家ID") @PathVariable Long id,
            Authentication authentication) {

        try {
            Object statistics = merchantService.getMerchantStatistics(id);
            return Result.success("查询成功", statistics);
        } catch (Exception e) {
            log.error("获取商家统计信息失败，商家ID: {}", id, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除商家
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "批量删除商家", description = "批量删除商家")
    public Result<Boolean> deleteMerchantsBatch(
            @Parameter(description = "商家ID列表") @RequestBody
            @jakarta.validation.constraints.NotNull(message = "商家ID列表不能为空") 
            @jakarta.validation.constraints.NotEmpty(message = "商家ID列表不能为空") java.util.List<Long> ids) {

        if (ids.size() > 100) {
            return Result.badRequest("批量删除数量不能超过100个");
        }

        try {
            int successCount = 0;
            for (Long id : ids) {
                if (merchantService.deleteMerchant(id)) {
                    successCount++;
                }
            }
            log.info("批量删除商家完成, 成功: {}/{}", successCount, ids.size());
            return Result.success(String.format("批量删除商家成功: %d/%d", successCount, ids.size()), true);
        } catch (Exception e) {
            log.error("批量删除商家失败, IDs: {}", ids, e);
            return Result.error("批量删除商家失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新商家状态
     */
    @PatchMapping("/batch/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "批量更新商家状态", description = "批量启用或禁用商家")
    public Result<Boolean> updateMerchantStatusBatch(
            @Parameter(description = "商家ID列表") @RequestParam
            @jakarta.validation.constraints.NotNull(message = "商家ID列表不能为空") java.util.List<Long> ids,
            @Parameter(description = "商家状态") @RequestParam
            @jakarta.validation.constraints.NotNull(message = "商家状态不能为空") Integer status) {

        if (ids == null || ids.isEmpty()) {
            return Result.badRequest("商家ID列表不能为空");
        }

        if (ids.size() > 100) {
            return Result.badRequest("批量操作数量不能超过100个");
        }

        try {
            int successCount = 0;
            for (Long id : ids) {
                if (merchantService.updateMerchantStatus(id, status)) {
                    successCount++;
                }
            }
            log.info("批量更新商家状态完成, 成功: {}/{}", successCount, ids.size());
            return Result.success(String.format("批量更新商家状态成功: %d/%d", successCount, ids.size()), true);
        } catch (Exception e) {
            log.error("批量更新商家状态失败, IDs: {}", ids, e);
            return Result.error("批量更新商家状态失败: " + e.getMessage());
        }
    }

    /**
     * 批量审核通过商家
     */
    @PostMapping("/batch/approve")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    @Operation(summary = "批量审核通过商家", description = "批量审核通过商家的入驻申请")
    public Result<Boolean> approveMerchantsBatch(
            @Parameter(description = "商家ID列表") @RequestBody
            @jakarta.validation.constraints.NotNull(message = "商家ID列表不能为空")
            @jakarta.validation.constraints.NotEmpty(message = "商家ID列表不能为空") java.util.List<Long> ids,
            @Parameter(description = "审核备注") @RequestParam(required = false) String remark) {

        if (ids.size() > 100) {
            return Result.badRequest("批量操作数量不能超过100个");
        }

        try {
            int successCount = 0;
            for (Long id : ids) {
                if (merchantService.approveMerchant(id, remark)) {
                    successCount++;
                }
            }
            log.info("批量审核通过商家完成, 成功: {}/{}", successCount, ids.size());
            return Result.success(String.format("批量审核通过商家成功: %d/%d", successCount, ids.size()), true);
        } catch (Exception e) {
            log.error("批量审核通过商家失败, IDs: {}", ids, e);
            return Result.error("批量审核通过商家失败: " + e.getMessage());
        }
    }
}
