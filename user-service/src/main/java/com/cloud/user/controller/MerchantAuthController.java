package com.cloud.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.MerchantAuthDTO;
import com.cloud.common.domain.dto.MerchantRegisterRequestDTO;
import com.cloud.common.utils.UserContextUtil;
import com.cloud.user.module.entity.MerchantAuth;
import com.cloud.user.service.FileUploadService;
import com.cloud.user.service.MerchantAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 商家认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/merchant/auth")
@RequiredArgsConstructor
@Tag(name = "商家认证", description = "商家认证申请与审核相关接口")
public class MerchantAuthController {

    private final MerchantAuthService merchantAuthService;
    private final FileUploadService fileUploadService;

    /**
     * 提交商家认证申请
     *
     * @param request 认证申请信息
     * @return 认证信息
     */
    @PostMapping("/apply")
    @Operation(summary = "提交商家认证申请", description = "提交商家认证申请，需要上传营业执照和身份证正反面图片")
    @ApiResponse(responseCode = "200", description = "提交成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<MerchantAuthDTO> submitAuthApplication(
            @Parameter(description = "认证申请信息") @Valid @ModelAttribute MerchantRegisterRequestDTO request) {
        try {
            log.info("提交商家认证申请, username: {}", request.getUsername());

            // 获取当前用户ID
            Long currentUserId = UserContextUtil.getCurrentUserId();
            if (currentUserId == null) {
                log.warn("提交认证申请失败：用户未登录");
                return Result.error("用户未登录");
            }

            // 提交认证申请
            MerchantAuth merchantAuth = merchantAuthService.submitAuthApplication(
                    currentUserId,
                    request.getNickname(), // 店铺名称使用昵称字段
                    request.getBusinessLicenseNumber(),
                    request.getBusinessLicenseFile(),
                    request.getIdCardFrontFile(),
                    request.getIdCardBackFile(),
                    request.getContactPhone(),
                    request.getContactAddress()
            );

            // 转换为DTO并返回
            MerchantAuthDTO merchantAuthDTO = merchantAuthService.convertToDTO(merchantAuth);
            return Result.success("提交成功，请等待管理员审核", merchantAuthDTO);
        } catch (IllegalStateException e) {
            log.error("提交商家认证申请失败：用户状态异常", e);
            return Result.error("提交失败: " + e.getMessage());
        } catch (MaxUploadSizeExceededException e) {
            log.error("提交商家认证申请失败：文件大小超出限制", e);
            return Result.error("提交失败: 文件大小超出限制");
        } catch (Exception e) {
            log.error("提交商家认证申请失败", e);
            return Result.error("提交失败: " + e.getMessage());
        }
    }

    /**
     * 查询当前用户的认证信息
     *
     * @return 认证信息
     */
    @GetMapping("/info")
    @Operation(summary = "查询当前用户的认证信息", description = "查询当前用户的认证信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<MerchantAuthDTO> getAuthInfo() {
        try {
            // 获取当前用户ID
            Long currentUserId = UserContextUtil.getCurrentUserId();
            if (currentUserId == null) {
                log.warn("查询认证信息失败：用户未登录");
                return Result.error("用户未登录");
            }

            // 查询认证信息
            MerchantAuth merchantAuth = merchantAuthService.getByUserId(currentUserId);
            if (merchantAuth == null) {
                log.warn("未找到认证信息, userId: {}", currentUserId);
                return Result.error("未找到认证信息");
            }

            // 转换为DTO并返回
            MerchantAuthDTO merchantAuthDTO = merchantAuthService.convertToDTO(merchantAuth);
            return Result.success(merchantAuthDTO);
        } catch (Exception e) {
            log.error("查询认证信息失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询待审核的商家认证申请（管理员接口）
     *
     * @param page 页码
     * @param size 每页数量
     * @return 认证信息分页结果
     */
    @GetMapping("/pending")
    @Operation(summary = "分页查询待审核的商家认证申请", description = "分页查询待审核的商家认证申请，仅管理员可访问")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    @PreAuthorize("hasAuthority('ADMIN')")
    public Result<IPage<MerchantAuthDTO>> getPendingAuthApplications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("查询待审核的商家认证申请, page: {}, size: {}", page, size);

            if (page <= 0) {
                log.warn("查询失败：页码必须大于0, page: {}", page);
                return Result.error("页码必须大于0");
            }
            
            if (size <= 0) {
                log.warn("查询失败：每页数量必须大于0, size: {}", size);
                return Result.error("每页数量必须大于0");
            }

            // 查询待审核的认证申请
            IPage<MerchantAuth> merchantAuthPage = merchantAuthService.getPendingAuthApplications(page, size);

            // 转换为DTO分页结果
            IPage<MerchantAuthDTO> merchantAuthDTOPage = merchantAuthPage.convert(merchantAuthService::convertToDTO);

            return Result.success(merchantAuthDTOPage);
        } catch (Exception e) {
            log.error("查询待审核的商家认证申请失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 审核商家认证申请（管理员接口）
     *
     * @param authId 认证ID
     * @param status 审核状态 1-通过，2-拒绝
     * @param remark 审核备注
     * @return 操作结果
     */
    @PutMapping("/audit/{authId}")
    @Operation(summary = "审核商家认证申请", description = "审核商家认证申请，仅管理员可访问")
    @ApiResponse(responseCode = "200", description = "审核成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    @PreAuthorize("hasAuthority('ADMIN')")
    public Result<?> auditMerchantAuth(
            @Parameter(description = "认证ID") @PathVariable Long authId,
            @Parameter(description = "审核状态 1-通过，2-拒绝") @RequestParam Integer status,
            @Parameter(description = "审核备注") @RequestParam(required = false) String remark) {
        try {
            log.info("审核商家认证申请, authId: {}, status: {}", authId, status);

            if (authId == null) {
                log.warn("审核失败：认证ID不能为空");
                return Result.error("认证ID不能为空");
            }
            
            if (status == null || (status != 1 && status != 2)) {
                log.warn("审核失败：审核状态无效, status: {}", status);
                return Result.error("审核状态无效，应为1（通过）或2（拒绝）");
            }

            // 执行审核
            boolean result = merchantAuthService.auditMerchantAuth(authId, status, remark);
            if (result) {
                log.info("审核商家认证申请成功, authId: {}, status: {}", authId, status);
                return Result.success("审核成功");
            } else {
                log.warn("审核失败，认证申请不存在, authId: {}", authId);
                return Result.error("审核失败，认证申请不存在");
            }
        } catch (Exception e) {
            log.error("审核商家认证申请失败", e);
            return Result.error("审核失败: " + e.getMessage());
        }
    }
}