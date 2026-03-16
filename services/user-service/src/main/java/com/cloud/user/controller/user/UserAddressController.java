package com.cloud.user.controller.user;

import com.cloud.common.domain.dto.user.UserAddressDTO;
import com.cloud.common.domain.dto.user.UserAddressPageDTO;
import com.cloud.common.domain.dto.user.UserAddressRequestDTO;
import com.cloud.common.domain.vo.UserAddressVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.user.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/user/address")
@RequiredArgsConstructor
@Tag(name = "User Address", description = "User address APIs")
public class UserAddressController {

  private final UserAddressService userAddressService;

  @PostMapping("/add/{userId}")
  @Operation(summary = "Add user address", description = "Add a new address for user")
  public Result<UserAddressDTO> addAddress(
      @PathVariable("userId")
          @Parameter(description = "User ID")
          @NotNull(message = "user id is required")
          Long userId,
      @RequestBody
          @Parameter(description = "Address payload")
          @Valid
          @NotNull(message = "address payload is required")
          UserAddressRequestDTO userAddressRequestDTO,
      Authentication authentication) {
    if (!SecurityPermissionUtils.isAdminOrOwner(authentication, userId)) {
      return Result.forbidden("no permission to add address");
    }

    UserAddressDTO created = userAddressService.createAddress(userId, userAddressRequestDTO);
    return Result.success("address created", created);
  }

  @PutMapping("/update/{addressId}")
  @Operation(summary = "Update user address", description = "Update address by address ID")
  public Result<UserAddressDTO> updateAddress(
      @PathVariable("addressId")
          @Parameter(description = "Address ID")
          @NotNull(message = "address id is required")
          Long addressId,
      @RequestBody
          @Parameter(description = "Address payload")
          @Valid
          @NotNull(message = "address payload is required")
          UserAddressRequestDTO userAddressRequestDTO,
      Authentication authentication) {
    UserAddressDTO existingAddress = userAddressService.getAddressById(addressId);
    if (existingAddress == null) {
      return Result.notFound("address not found");
    }

    if (!SecurityPermissionUtils.isAdminOrOwner(authentication, existingAddress.getUserId())) {
      return Result.forbidden("no permission to update address");
    }

    UserAddressDTO updated = userAddressService.updateAddress(addressId, userAddressRequestDTO);
    return Result.success("address updated", updated);
  }

  @DeleteMapping("/delete/{addressId}")
  @Operation(summary = "Delete user address", description = "Delete address by address ID")
  public Result<Boolean> deleteAddress(
      @PathVariable("addressId")
          @Parameter(description = "Address ID")
          @NotNull(message = "address id is required")
          Long addressId,
      Authentication authentication) {
    UserAddressDTO existingAddress = userAddressService.getAddressById(addressId);
    if (existingAddress == null) {
      return Result.notFound("address not found");
    }

    if (!SecurityPermissionUtils.isAdminOrOwner(authentication, existingAddress.getUserId())) {
      return Result.forbidden("no permission to delete address");
    }

    boolean result = userAddressService.removeById(addressId);
    return Result.success("address deleted", result);
  }

  @GetMapping("/list/{userId}")
  @Operation(summary = "List user addresses", description = "List all addresses for one user")
  public Result<List<UserAddressVO>> getAddressList(
      @PathVariable("userId")
          @Parameter(description = "User ID")
          @NotNull(message = "user id is required")
          Long userId,
      Authentication authentication) {
    if (!SecurityPermissionUtils.isAdminOrOwner(authentication, userId)) {
      return Result.forbidden("no permission to query addresses");
    }

    List<UserAddressVO> result = userAddressService.listAddressesByUserId(userId);
    return Result.success(result);
  }

  @GetMapping("/default/{userId}")
  @Operation(summary = "Get default address", description = "Get default address for one user")
  public Result<UserAddressVO> getDefaultAddress(
      @PathVariable("userId")
          @Parameter(description = "User ID")
          @NotNull(message = "user id is required")
          Long userId,
      Authentication authentication) {
    if (!SecurityPermissionUtils.isAdminOrOwner(authentication, userId)) {
      return Result.forbidden("no permission to query default address");
    }

    UserAddressVO userAddress = userAddressService.getDefaultAddress(userId);
    if (userAddress == null) {
      return Result.success("default address not found", null);
    }

    return Result.success(userAddress);
  }

  @PostMapping("/page")
  @Operation(summary = "Page user addresses", description = "Page query user addresses")
  public Result<PageResult<UserAddressVO>> pageUserAddress(
      @RequestBody
          @Parameter(description = "Page query payload")
          @Valid
          @NotNull(message = "page query payload is required")
          UserAddressPageDTO pageDTO,
      Authentication authentication) {
    try {
      if (pageDTO.getUserId() != null
          && !SecurityPermissionUtils.isAdminOrOwner(authentication, pageDTO.getUserId())) {
        return Result.forbidden("no permission to query this user's addresses");
      }
      if (pageDTO.getUserId() == null && !SecurityPermissionUtils.isAdmin(authentication)) {
        return Result.forbidden("no permission to query all addresses");
      }

      PageResult<UserAddressVO> pageResult = userAddressService.pageAddresses(pageDTO);
      return Result.success(pageResult);
    } catch (Exception e) {
      log.error("Failed to page user addresses", e);
      return Result.error("failed to page user addresses");
    }
  }

  @DeleteMapping("/deleteBatch")
  @Operation(
      summary = "Batch delete addresses",
      description = "Batch delete addresses by address IDs")
  public Result<Boolean> deleteAddressBatch(
      @RequestBody
          @Parameter(description = "Address IDs")
          @NotNull(message = "address ids are required")
          List<Long> addressIds,
      Authentication authentication) {
    if (addressIds.isEmpty()) {
      return Result.badRequest("address ids cannot be empty");
    }
    if (addressIds.size() > 100) {
      return Result.badRequest("batch size cannot exceed 100");
    }

    int successCount = userAddressService.deleteAddressBatch(addressIds, authentication);
    String message =
        String.format("batch delete completed: %d/%d", successCount, addressIds.size());
    return Result.success(message, true);
  }

  @PutMapping("/updateBatch")
  @Operation(
      summary = "Batch update addresses",
      description = "Batch update addresses by address payload list")
  public Result<Boolean> updateAddressBatch(
      @RequestBody
          @Parameter(description = "Address payload list")
          @Valid
          @NotNull(message = "address payload list is required")
          List<UserAddressRequestDTO> addressList,
      Authentication authentication) {
    if (addressList.isEmpty()) {
      return Result.badRequest("address payload list cannot be empty");
    }
    if (addressList.size() > 100) {
      return Result.badRequest("batch size cannot exceed 100");
    }

    int successCount = userAddressService.updateAddressBatch(addressList, authentication);
    String message =
        String.format("batch update completed: %d/%d", successCount, addressList.size());
    return Result.success(message, true);
  }
}
