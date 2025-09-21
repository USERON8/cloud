package com.cloud.user.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.domain.dto.user.MerchantShopDTO;
import com.cloud.common.domain.vo.MerchantAuthVO;
import com.cloud.common.domain.vo.MerchantVO;
import com.cloud.common.domain.vo.UserAddressVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.utils.PageUtils;
import com.cloud.user.constants.OAuth2Permissions;
import com.cloud.user.converter.MerchantAuthConverter;
import com.cloud.user.converter.MerchantConverter;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.module.entity.Merchant;
import com.cloud.user.module.entity.MerchantAuth;
import com.cloud.user.module.entity.User;
import com.cloud.user.module.entity.UserAddress;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.service.MerchantAuthService;
import com.cloud.user.service.MerchantService;
import com.cloud.user.service.UserService;
import com.cloud.user.service.UserAddressService;
import com.cloud.user.converter.UserAddressConverter;
import com.cloud.user.module.dto.MerchantPageDTO;
import com.cloud.user.module.dto.MerchantAuthPageDTO;
import com.cloud.user.module.dto.UserAddressPageDTO;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/merchant/query")
@RequiredArgsConstructor
@Tag(name = "商家查询", description = "商家信息查询相关操作")
public class MerchantQueryController {
    private final MerchantService merchantService;
    private final MerchantAuthService merchantAuthService;
    private final UserAddressService userAddressService;

    private final MerchantConverter merchantConverter = MerchantConverter.INSTANCE;
    private final MerchantAuthConverter merchantAuthConverter = MerchantAuthConverter.INSTANCE;
    private final UserAddressConverter userAddressConverter = UserAddressConverter.INSTANCE;

    @GetMapping("/getMerchantById/{id}")
    @Operation(summary = "根据ID获取商家信息", description = "根据商家ID获取详细信息")
    public Result<MerchantDTO> getMerchantById(@PathVariable("id")
                                               @Parameter(description = "商家ID")
                                               @NotNull(message = "商家ID不能为空") Long id,
                                               Authentication authentication) {

        // 使用统一的权限检查工具（商家权限检查）
        if (!SecurityPermissionUtils.isAdminOrMerchantOwner(authentication, id)) {
            return Result.forbidden("只有商家自己或管理员可以查看商家信息");
        }

        try {
            Merchant merchant = merchantService.getById(id);
            if (merchant == null) {
                return Result.notFound("商家不存在");
            }
            MerchantDTO merchantDTO = merchantConverter.toDTO(merchant);
            return Result.success("查询成功", merchantDTO);
        } catch (Exception e) {
            log.error("获取商家信息失败，商家ID: {}", id, e);
            return Result.systemError("查询失败");
        }
    }

    @GetMapping("/getAllMerchants")
    @Operation(summary = "获取所有商家", description = "获取系统中所有商家的信息")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<List<MerchantDTO>> getAllMerchants(Authentication authentication) {


        try {
            List<Merchant> merchants = merchantService.list();
            List<MerchantDTO> merchantDTOs = merchantConverter.toDTOList(merchants);
            return Result.success("查询成功", merchantDTOs);
        } catch (Exception e) {
            log.error("获取所有商家信息失败", e);
            return Result.systemError("查询失败");
        }
    }

    @GetMapping("/getShopsByMerchantId/{merchantId}")
    @Operation(summary = "根据商家ID获取店铺列表", description = "根据商家ID获取其所有店铺")
    public Result<List<MerchantShopDTO>> getShopsByMerchantId(@PathVariable("merchantId")
                                                              @Parameter(description = "商家ID")
                                                              @NotNull(message = "商家ID不能为空") Long merchantId,
                                                              Authentication authentication) {

        // 权限检查：只有管理员或商家自己可以查看店铺列表
        if (!SecurityPermissionUtils.isAdminOrMerchantOwner(authentication, merchantId)) {
            return Result.error("无权执行此操作");
        }

        // 注意：店铺信息应该在商家服务中管理，这里返回空列表作为占位符
        log.info("商家店铺查询功能尚未实现，商家ID: {}", merchantId);
        return Result.success("店铺功能尚未实现", List.of());
    }

    @GetMapping("/getShopById/{id}")
    @Operation(summary = "根据ID获取店铺信息", description = "根据店铺ID获取详细信息")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<MerchantShopDTO> getShopById(@PathVariable("id")
                                               @Parameter(description = "店铺ID")
                                               @NotNull(message = "店铺ID不能为空") Long id,
                                               Authentication authentication) {

        // 权限检查：这里简化处理，只有管理员可以查看任意店铺信息
        if (!SecurityPermissionUtils.isAdmin(authentication)) {
            return Result.forbidden("只有管理员可以查看店铺信息");
        }

        log.info("店铺查询功能尚未实现，店铺 ID: {}", id);
        return Result.success("店铺功能尚未实现", null);
    }

    @GetMapping("/getAllShops")
    @Operation(summary = "获取所有店铺", description = "获取系统中所有店铺的信息")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<List<MerchantShopDTO>> getAllShops(Authentication authentication) {


        log.info("获取所有店铺功能尚未实现");
        return Result.success("店铺功能尚未实现", List.of());
    }

    @GetMapping("/getPendingAuths")
    @Operation(summary = "获取待审核认证列表", description = "获取所有待审核的商家认证申请")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<List<MerchantAuthDTO>> getPendingAuths(Authentication authentication) {


        try {
            List<MerchantAuth> pendingAuths = merchantAuthService.list(
                    new LambdaQueryWrapper<MerchantAuth>()
                            .eq(MerchantAuth::getAuthStatus, 0) // 0-待审核
                            .orderByDesc(MerchantAuth::getCreatedAt)
            );
            List<MerchantAuthDTO> authDTOs = merchantAuthConverter.toDTOList(pendingAuths);
            return Result.success(authDTOs);
        } catch (Exception e) {
            log.error("获取待审核认证列表失败", e);
            return Result.error("查询失败");
        }
    }

    @GetMapping("/getAuthById/{id}")
    @Operation(summary = "根据ID获取认证信息", description = "根据认证ID获取详细信息")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<MerchantAuthDTO> getAuthById(@PathVariable("id")
                                               @Parameter(description = "认证ID")
                                               @NotNull(message = "认证ID不能为空") Long id,
                                               Authentication authentication) {


        try {
            MerchantAuth auth = merchantAuthService.getById(id);
            if (auth == null) {
                return Result.error("认证信息不存在");
            }
            MerchantAuthDTO authDTO = merchantAuthConverter.toDTO(auth);
            return Result.success("查询成功", authDTO);
        } catch (Exception e) {
            log.error("获取认证信息失败，认证ID: {}", id, e);
            return Result.error("查询失败");
        }
    }

    @GetMapping("/getAllAuths")
    @Operation(summary = "获取所有认证信息", description = "获取系统中所有商家认证信息")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<List<MerchantAuthDTO>> getAllAuths(Authentication authentication) {


        try {
            List<MerchantAuth> auths = merchantAuthService.list();
            List<MerchantAuthDTO> authDTOs = merchantAuthConverter.toDTOList(auths);
            return Result.success(authDTOs);
        } catch (Exception e) {
            log.error("获取所有认证信息失败", e);
            return Result.error("查询失败");
        }
    }

    // 新增商家信息分页查询
    @PostMapping("/merchant/page")
    @Operation(summary = "分页查询商家信息", description = "分页查询商家信息")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<PageResult<MerchantVO>> pageMerchant(@RequestBody
                                                       @Parameter(description = "分页查询条件")
                                                       @Valid @NotNull(message = "分页查询条件不能为空") MerchantPageDTO pageDTO,
                                                       Authentication authentication) {


        try {
            log.info("分页查询商家信息, page: {}, size: {}, merchantName: {}, status: {}",
                    pageDTO.getCurrent(), pageDTO.getSize(), pageDTO.getMerchantName(), pageDTO.getStatus());

            // 构造分页对象
            Page<Merchant> page = PageUtils.buildPage(pageDTO);

            // 构造查询条件
            LambdaQueryWrapper<Merchant> queryWrapper = Wrappers.lambdaQuery();
            if (pageDTO.getMerchantName() != null && !pageDTO.getMerchantName().isEmpty()) {
                queryWrapper.like(Merchant::getMerchantName, pageDTO.getMerchantName());
            }
            if (pageDTO.getStatus() != null) {
                queryWrapper.eq(Merchant::getStatus, pageDTO.getStatus());
            }
            queryWrapper.orderByDesc(Merchant::getCreatedAt);

            // 执行分页查询
            Page<Merchant> resultPage = merchantService.page(page, queryWrapper);

            // 转换为VO列表
            List<MerchantVO> merchantVOList = merchantConverter.toVOList(resultPage.getRecords());

            // 封装分页结果
            PageResult<MerchantVO> pageResult = PageResult.of(
                    resultPage.getCurrent(),
                    resultPage.getSize(),
                    resultPage.getTotal(),
                    merchantVOList
            );

            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("分页查询商家信息异常", e);
            return Result.error("查询失败");
        }
    }

    // 新增商家认证信息分页查询
    @PostMapping("/auth/page")
    @Operation(summary = "分页查询商家认证信息", description = "分页查询商家认证信息")
    @PreAuthorize(OAuth2Permissions.HAS_ROLE_ADMIN)
    public Result<PageResult<MerchantAuthVO>> pageMerchantAuth(@RequestBody
                                                               @Parameter(description = "分页查询条件")
                                                               @Valid @NotNull(message = "分页查询条件不能为空") MerchantAuthPageDTO pageDTO,
                                                               Authentication authentication) {


        try {
            log.info("分页查询商家认证信息, page: {}, size: {}, merchantName: {}, authStatus: {}",
                    pageDTO.getCurrent(), pageDTO.getSize(), pageDTO.getMerchantName(), pageDTO.getAuthStatus());

            // 构造分页对象
            Page<MerchantAuth> page = PageUtils.buildPage(pageDTO);

            // 构造查询条件
            LambdaQueryWrapper<MerchantAuth> queryWrapper = Wrappers.lambdaQuery();
            if (pageDTO.getAuthStatus() != null) {
                queryWrapper.eq(MerchantAuth::getAuthStatus, pageDTO.getAuthStatus());
            }
            queryWrapper.orderByDesc(MerchantAuth::getCreatedAt);

            // 执行分页查询
            Page<MerchantAuth> resultPage = merchantAuthService.page(page, queryWrapper);

            // 转换为VO列表
            List<MerchantAuthVO> merchantAuthVOList = merchantAuthConverter.toVOList(resultPage.getRecords());

            // 封装分页结果
            PageResult<MerchantAuthVO> pageResult = PageResult.of(
                    resultPage.getCurrent(),
                    resultPage.getSize(),
                    resultPage.getTotal(),
                    merchantAuthVOList
            );

            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("分页查询商家认证信息异常", e);
            return Result.error("查询失败");
        }
    }

    // 新增用户地址信息分页查询
    @PostMapping("/address/page")
    @Operation(summary = "分页查询用户地址信息", description = "分页查询用户地址信息")
    public Result<PageResult<UserAddressVO>> pageUserAddress(@RequestBody
                                                             @Parameter(description = "分页查询条件")
                                                             @Valid @NotNull(message = "分页查询条件不能为空") UserAddressPageDTO pageDTO,
                                                             Authentication authentication) {

        // 使用统一的权限检查工具
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, pageDTO.getUserId())) {
            return Result.forbidden("无权限查询此用户地址信息");
        }

        try {
            log.info("分页查询用户地址信息, page: {}, size: {}, userId: {}, consignee: {}",
                    pageDTO.getCurrent(), pageDTO.getSize(), pageDTO.getUserId(), pageDTO.getConsignee());

            // 构造分页对象
            Page<UserAddress> page = PageUtils.buildPage(pageDTO);

            // 构造查询条件
            LambdaQueryWrapper<UserAddress> queryWrapper = Wrappers.lambdaQuery();
            if (pageDTO.getUserId() != null) {
                queryWrapper.eq(UserAddress::getUserId, pageDTO.getUserId());
            }
            if (pageDTO.getConsignee() != null && !pageDTO.getConsignee().isEmpty()) {
                queryWrapper.like(UserAddress::getConsignee, pageDTO.getConsignee());
            }
            queryWrapper.orderByDesc(UserAddress::getCreatedAt);

            // 执行分页查询
            Page<UserAddress> resultPage = userAddressService.page(page, queryWrapper);

            // 转换为VO列表
            List<UserAddressVO> userAddressVOList = userAddressConverter.toVOList(resultPage.getRecords());

            // 封装分页结果
            PageResult<UserAddressVO> pageResult = PageResult.of(
                    resultPage.getCurrent(),
                    resultPage.getSize(),
                    resultPage.getTotal(),
                    userAddressVOList
            );

            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("分页查询用户地址信息异常", e);
            return Result.error("查询失败");
        }
    }


}