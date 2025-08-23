package com.cloud.user.controller;

import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.user.UserAddressDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.utils.BeanUtils;
import com.cloud.user.converter.UserAddressConverter;
import com.cloud.user.module.entity.UserAddress;
import com.cloud.user.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/address")
@RequiredArgsConstructor
@Tag(name = "地址管理", description = "用户地址管理接口")
public class AddressController {
    private final UserAddressService userAddressService;
    private final UserAddressConverter userAddressConvert;

    @PostMapping("/add")
    @Operation(summary = "新增地址", description = "为当前用户新增收货地址")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户地址信息", required = true)
    @Parameters({
            @Parameter(name = "X-User-ID", description = "当前用户ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "新增成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> add(@RequestBody UserAddressDTO userAddressDTO,
                              @RequestHeader("X-User-ID") String currentUserId) {
        try {
            log.info("开始新增地址, 操作用户ID: {}", currentUserId);

            // 参数验证
            if (currentUserId == null || currentUserId.isEmpty()) {
                log.warn("新增地址失败: 操作用户ID为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "操作用户ID不能为空");
            }

            if (userAddressDTO == null) {
                log.warn("新增地址失败: 地址信息为空, 操作用户ID: {}", currentUserId);
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "地址信息不能为空");
            }

            Long userId = Long.valueOf(currentUserId);
            UserAddress userAddress = userAddressConvert.toEntity(userAddressDTO);
            userAddress.setUserId(userId);

            boolean result = userAddressService.saveOrUpdate(userAddress);
            if (result) {
                log.info("新增地址成功, 地址ID: {}, 操作用户ID: {}", userAddress.getId(), currentUserId);
                return Result.success("新增地址成功");
            } else {
                log.error("新增地址失败, 操作用户ID: {}", currentUserId);
                return Result.error("新增地址失败");
            }
        } catch (NumberFormatException e) {
            log.error("新增地址失败: 用户ID格式错误, 操作用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID格式错误");
        } catch (BusinessException e) {
            log.error("新增地址失败: 业务异常, 操作用户ID: {}", currentUserId, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("新增地址失败: 系统异常, 操作用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "新增地址失败: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    @Operation(summary = "更新地址", description = "更新指定地址信息")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户地址信息", required = true)
    @Parameters({
            @Parameter(name = "X-User-ID", description = "当前用户ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> update(@RequestBody UserAddressDTO userAddressDTO,
                                 @RequestHeader("X-User-ID") String currentUserId) {
        try {
            log.info("开始更新地址, 地址ID: {}, 操作用户ID: {}", userAddressDTO.getId(), currentUserId);

            // 参数验证
            if (currentUserId == null || currentUserId.isEmpty()) {
                log.warn("更新地址失败: 操作用户ID为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "操作用户ID不能为空");
            }

            if (userAddressDTO == null || userAddressDTO.getId() == null) {
                log.warn("更新地址失败: 地址信息不完整, 操作用户ID: {}", currentUserId);
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "地址信息不完整");
            }

            Long userId = Long.valueOf(currentUserId);
            UserAddress userAddress = userAddressConvert.toEntity(userAddressDTO);
            userAddress.setUserId(userId);

            boolean result = userAddressService.saveOrUpdate(userAddress);
            if (result) {
                log.info("更新地址成功, 地址ID: {}, 操作用户ID: {}", userAddress.getId(), currentUserId);
                return Result.success("更新地址成功");
            } else {
                log.error("更新地址失败, 地址ID: {}, 操作用户ID: {}", userAddress.getId(), currentUserId);
                return Result.error("更新地址失败");
            }
        } catch (NumberFormatException e) {
            log.error("更新地址失败: 用户ID格式错误, 操作用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID格式错误");
        } catch (BusinessException e) {
            log.error("更新地址失败: 业务异常, 操作用户ID: {}", currentUserId, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新地址失败: 系统异常, 操作用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "更新地址失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除地址", description = "删除指定地址")
    @Parameters({
            @Parameter(name = "id", description = "地址ID", required = true),
            @Parameter(name = "X-User-ID", description = "当前用户ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "删除成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<String> delete(@PathVariable Long id,
                                 @RequestHeader("X-User-ID") String currentUserId) {
        try {
            log.info("开始删除地址, 地址ID: {}, 操作用户ID: {}", id, currentUserId);

            // 参数验证
            if (currentUserId == null || currentUserId.isEmpty()) {
                log.warn("删除地址失败: 操作用户ID为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "操作用户ID不能为空");
            }

            if (id == null) {
                log.warn("删除地址失败: 地址ID为空, 操作用户ID: {}", currentUserId);
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "地址ID不能为空");
            }

            Long userId = Long.valueOf(currentUserId);
            // 检查地址是否存在且属于当前用户
            UserAddress userAddress = userAddressService.getById(id);
            if (userAddress == null) {
                log.warn("删除地址失败: 地址不存在, 地址ID: {}, 操作用户ID: {}", id, currentUserId);
                return Result.error(ResultCode.RESOURCE_NOT_FOUND.getCode(), "地址不存在");
            }

            if (!userAddress.getUserId().equals(userId)) {
                log.warn("删除地址失败: 无权限操作, 地址ID: {}, 操作用户ID: {}", id, currentUserId);
                return Result.error(ResultCode.FORBIDDEN.getCode(), "无权限操作该地址");
            }

            boolean result = userAddressService.removeById(id);
            if (result) {
                log.info("删除地址成功, 地址ID: {}, 操作用户ID: {}", id, currentUserId);
                return Result.success("删除地址成功");
            } else {
                log.error("删除地址失败, 地址ID: {}, 操作用户ID: {}", id, currentUserId);
                return Result.error("删除地址失败");
            }
        } catch (NumberFormatException e) {
            log.error("删除地址失败: 用户ID格式错误, 操作用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID格式错误");
        } catch (BusinessException e) {
            log.error("删除地址失败: 业务异常, 操作用户ID: {}", currentUserId, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除地址失败: 系统异常, 操作用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "删除地址失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    @Operation(summary = "获取地址列表", description = "获取当前用户的所有地址列表")
    @Parameters({
            @Parameter(name = "X-User-ID", description = "当前用户ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<List<UserAddressDTO>> list(@RequestHeader("X-User-ID") String currentUserId) {
        try {
            log.info("开始获取地址列表, 操作用户ID: {}", currentUserId);

            // 参数验证
            if (currentUserId == null || currentUserId.isEmpty()) {
                log.warn("获取地址列表失败: 操作用户ID为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "操作用户ID不能为空");
            }

            Long userId = Long.valueOf(currentUserId);
            List<UserAddress> userAddressList = userAddressService.getAddressByUserId(userId);
            List<UserAddressDTO> userAddressDTOList = userAddressList.stream()
                    .map(userAddress -> {
                        UserAddressDTO userAddressDTO = new UserAddressDTO();
                        BeanUtils.copyProperties(userAddress, userAddressDTO);
                        return userAddressDTO;
                    })
                    .toList();
            // 获取用户地址列表
            // 注意：这里需要修改返回类型，应该返回List<UserAddressDTO>而不是UserAddressDTO
            return Result.success("获取地址列表成功", userAddressDTOList);
        } catch (NumberFormatException e) {
            log.error("获取地址列表失败: 用户ID格式错误, 操作用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID格式错误");
        } catch (BusinessException e) {
            log.error("获取地址列表失败: 业务异常, 操作用户ID: {}", currentUserId, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取地址列表失败: 系统异常, 操作用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "获取地址列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取地址详情", description = "根据地址ID获取地址详情")
    @Parameters({
            @Parameter(name = "id", description = "地址ID", required = true),
            @Parameter(name = "X-User-ID", description = "当前用户ID", required = true)
    })
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<UserAddressDTO> get(@PathVariable Long id,
                                      @RequestHeader("X-User-ID") String currentUserId) {
        try {
            log.info("开始获取地址详情, 地址ID: {}, 操作用户ID: {}", id, currentUserId);

            // 参数验证
            if (currentUserId == null || currentUserId.isEmpty()) {
                log.warn("获取地址详情失败: 操作用户ID为空");
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "操作用户ID不能为空");
            }

            if (id == null) {
                log.warn("获取地址详情失败: 地址ID为空, 操作用户ID: {}", currentUserId);
                return Result.error(ResultCode.PARAM_ERROR.getCode(), "地址ID不能为空");
            }

            Long userId = Long.valueOf(currentUserId);
            // 获取地址详情
            UserAddress userAddress = userAddressService.getById(id);
            if (userAddress == null) {
                log.warn("获取地址详情失败: 地址不存在, 地址ID: {}, 操作用户ID: {}", id, currentUserId);
                return Result.error(ResultCode.RESOURCE_NOT_FOUND.getCode(), "地址不存在");
            }

            // 检查地址是否属于当前用户
            if (!userAddress.getUserId().equals(userId)) {
                log.warn("获取地址详情失败: 无权限操作, 地址ID: {}, 操作用户ID: {}", id, currentUserId);
                return Result.error(ResultCode.FORBIDDEN.getCode(), "无权限操作该地址");
            }

            UserAddressDTO userAddressDTO = userAddressConvert.toDTO(userAddress);
            log.info("获取地址详情成功, 地址ID: {}, 操作用户ID: {}", id, currentUserId);
            return Result.success(userAddressDTO);
        } catch (NumberFormatException e) {
            log.error("获取地址详情失败: 用户ID格式错误, 操作用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "用户ID格式错误");
        } catch (BusinessException e) {
            log.error("获取地址详情失败: 业务异常, 操作用户ID: {}", currentUserId, e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取地址详情失败: 系统异常, 操作用户ID: {}", currentUserId, e);
            return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "获取地址详情失败: " + e.getMessage());
        }
    }
}