package com.cloud.user.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.user.UserAddressDTO;
import com.cloud.common.domain.dto.user.UserAddressRequestDTO;
import com.cloud.common.domain.vo.UserAddressVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.utils.PageUtils;
import com.cloud.user.converter.UserAddressConverter;
import com.cloud.common.domain.dto.user.UserAddressPageDTO;
import com.cloud.user.module.entity.UserAddress;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user/address")
@RequiredArgsConstructor
@Tag(name = "用户地址管理", description = "用户地址添加、更新、删除、查询等相关操作")
public class UserAddressController {
    private final UserAddressService userAddressService;
    private final UserAddressConverter userAddressConverter = UserAddressConverter.INSTANCE;

    /**
     * 添加用户地址
     *
     * @param userId                用户ID
     * @param userAddressRequestDTO 地址信息
     * @return 地址信息
     */
    @PostMapping("/add/{userId}")
    @Operation(summary = "添加用户地址", description = "为指定用户添加新的地址信息")
    public Result<UserAddressDTO> addAddress(
            @PathVariable("userId")
            @Parameter(description = "用户ID")
            @NotNull(message = "用户ID不能为空") Long userId,
            @RequestBody
            @Parameter(description = "地址信息")
            @Valid @NotNull(message = "地址信息不能为空") UserAddressRequestDTO userAddressRequestDTO,
            Authentication authentication) {

        // 使用统一的权限检查工具
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, userId)) {
            return Result.forbidden("无权限操作此用户地址");
        }

        log.info("添加用户地址, userId: {}", userId);

        UserAddress userAddress = userAddressConverter.toEntity(userAddressRequestDTO);
        userAddress.setUserId(userId);
        userAddress.setCreatedAt(LocalDateTime.now());
        userAddress.setUpdatedAt(LocalDateTime.now());

        // 如果设置为默认地址，需要将其他地址设为非默认
        if (userAddress.getIsDefault() != null && userAddress.getIsDefault() == 1) {
            setUserAddressNotDefault(userId);
        }

        userAddressService.save(userAddress);

        UserAddressDTO result = userAddressConverter.toDTO(userAddress);
        return Result.success("地址添加成功", result);
    }

    /**
     * 更新用户地址
     *
     * @param addressId             地址ID
     * @param userAddressRequestDTO 地址信息
     * @return 地址信息
     */
    @PutMapping("/update/{addressId}")
    @Operation(summary = "更新用户地址", description = "更新指定地址的信息")
    public Result<UserAddressDTO> updateAddress(
            @PathVariable("addressId")
            @Parameter(description = "地址ID")
            @NotNull(message = "地址ID不能为空") Long addressId,
            @RequestBody
            @Parameter(description = "地址信息")
            @Valid @NotNull(message = "地址信息不能为空") UserAddressRequestDTO userAddressRequestDTO,
            Authentication authentication) {

        // 检查地址是否存在以及权限
        UserAddress existingAddress = userAddressService.getById(addressId);
        if (existingAddress == null) {
            return Result.error("地址不存在");
        }

        // 使用统一的权限检查工具
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, existingAddress.getUserId())) {
            return Result.forbidden("无权限更新此地址");
        }

        log.info("更新用户地址, addressId: {}", addressId);

        UserAddress userAddress = userAddressConverter.toEntity(userAddressRequestDTO);
        userAddress.setId(addressId);
        userAddress.setUpdatedAt(LocalDateTime.now());

        // 如果设置为默认地址，需要将其他地址设为非默认
        if (userAddress.getIsDefault() != null && userAddress.getIsDefault() == 1) {
            setUserAddressNotDefault(existingAddress.getUserId());
        }

        userAddressService.updateById(userAddress);

        UserAddressDTO result = userAddressConverter.toDTO(userAddress);
        return Result.success("地址更新成功", result);
    }

    /**
     * 删除用户地址
     *
     * @param addressId 地址ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{addressId}")
    @Operation(summary = "删除用户地址", description = "删除指定的用户地址")
    public Result<Boolean> deleteAddress(@PathVariable("addressId")
                                         @Parameter(description = "地址ID")
                                         @NotNull(message = "地址ID不能为空") Long addressId,
                                         Authentication authentication) {

        // 检查地址是否存在以及权限
        UserAddress existingAddress = userAddressService.getById(addressId);
        if (existingAddress == null) {
            return Result.error("地址不存在");
        }

        // 使用统一的权限检查工具
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, existingAddress.getUserId())) {
            return Result.forbidden("无权限删除此地址");
        }

        log.info("删除用户地址, addressId: {}", addressId);
        boolean result = userAddressService.removeById(addressId);
        return Result.success("地址删除成功", result);
    }

    /**
     * 获取用户地址列表
     *
     * @param userId 用户ID
     * @return 地址列表
     */
    @GetMapping("/list/{userId}")
    @Operation(summary = "获取用户地址列表", description = "获取指定用户的所有地址信息")
    public Result<List<UserAddressVO>> getAddressList(@PathVariable("userId")
                                                      @Parameter(description = "用户ID")
                                                      @NotNull(message = "用户ID不能为空") Long userId,
                                                      Authentication authentication) {

        // 使用统一的权限检查工具
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, userId)) {
            return Result.forbidden("无权限查看此用户地址列表");
        }

        log.info("获取用户地址列表, userId: {}", userId);

        LambdaQueryWrapper<UserAddress> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(UserAddress::getUserId, userId);
        queryWrapper.orderByDesc(UserAddress::getIsDefault);
        queryWrapper.orderByDesc(UserAddress::getUpdatedAt);

        List<UserAddress> userAddresses = userAddressService.list(queryWrapper);
        List<UserAddressVO> result = userAddressConverter.toVOList(userAddresses);
        return Result.success(result);
    }

    /**
     * 获取用户默认地址
     *
     * @param userId 用户ID
     * @return 默认地址
     */
    @GetMapping("/default/{userId}")
    @Operation(summary = "获取用户默认地址", description = "获取指定用户的默认地址")
    public Result<UserAddressVO> getDefaultAddress(@PathVariable("userId")
                                                   @Parameter(description = "用户ID")
                                                   @NotNull(message = "用户ID不能为空") Long userId,
                                                   Authentication authentication) {

        // 使用统一的权限检查工具
        if (!SecurityPermissionUtils.isAdminOrOwner(authentication, userId)) {
            return Result.forbidden("无权限获取此用户默认地址");
        }

        log.info("获取用户默认地址, userId: {}", userId);

        LambdaQueryWrapper<UserAddress> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(UserAddress::getUserId, userId);
        queryWrapper.eq(UserAddress::getIsDefault, 1);

        UserAddress userAddress = userAddressService.getOne(queryWrapper);
        if (userAddress == null) {
            return Result.success("暂无默认地址", null);
        }

        UserAddressVO result = userAddressConverter.toVO(userAddress);
        return Result.success(result);
    }

    // 新增用户地址信息分页查询
    @PostMapping("/page")
    @Operation(summary = "分页查询用户地址信息", description = "分页查询用户地址信息")
    public Result<PageResult<UserAddressVO>> pageUserAddress(@RequestBody
                                                             @Parameter(description = "分页查询条件")
                                                             @Valid @NotNull(message = "分页查询条件不能为空") UserAddressPageDTO pageDTO,
                                                             Authentication authentication) {
        try {
            // 使用统一的权限检查工具
            if (!SecurityPermissionUtils.isAdminOrOwner(authentication, pageDTO.getUserId())) {
                return Result.forbidden("无权限查询此用户地址信息");
            }

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

    /**
     * 将用户其他地址设为非默认
     *
     * @param userId 用户ID
     */
    private void setUserAddressNotDefault(Long userId) {
        LambdaQueryWrapper<UserAddress> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(UserAddress::getUserId, userId);
        queryWrapper.eq(UserAddress::getIsDefault, 1);

        UserAddress userAddress = new UserAddress();
        userAddress.setIsDefault(0);
        userAddressService.update(userAddress, queryWrapper);
    }
}