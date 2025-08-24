package com.cloud.merchant.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.api.merchant.MerchantFeignClient;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.merchant.MerchantAuthDTO;
import com.cloud.common.domain.dto.merchant.MerchantDTO;
import com.cloud.common.domain.dto.merchant.MerchantShopDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.merchant.converter.MerchantConverter;
import com.cloud.merchant.module.entity.Merchant;
import com.cloud.merchant.module.entity.MerchantAuth;
import com.cloud.merchant.module.entity.MerchantShop;
import com.cloud.merchant.service.MerchantAuthService;
import com.cloud.merchant.service.MerchantService;
import com.cloud.merchant.service.MerchantShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家服务Feign客户端接口实现控制器
 * 实现商家服务对外提供的Feign接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MerchantFeignClientController implements MerchantFeignClient {

    private final MerchantService merchantService;
    private final MerchantShopService merchantShopService;
    private final MerchantAuthService merchantAuthService;
    private final MerchantConverter merchantConverter = MerchantConverter.INSTANCE;

    /**
     * 根据ID获取商家信息
     *
     * @param id 商家ID
     * @return 商家信息
     */
    @Override
    public Result<MerchantDTO> getMerchantById(Long id) {
        try {
            log.info("Feign调用：根据ID获取商家信息，商家ID: {}", id);

            Merchant merchant = merchantService.getById(id);
            if (merchant == null) {
                log.warn("商家不存在，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商家不存在");
            }

            MerchantDTO merchantDTO = merchantConverter.toDTO(merchant);
            log.info("Feign调用：获取商家信息成功，商家ID: {}", id);
            return Result.success(merchantDTO);
        } catch (Exception e) {
            log.error("Feign调用：获取商家信息失败，商家ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取商家信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有商家
     *
     * @return 商家列表
     */
    @Override
    public Result<List<MerchantDTO>> getAllMerchants() {
        try {
            log.info("Feign调用：获取所有商家");

            List<Merchant> merchants = merchantService.list();
            List<MerchantDTO> merchantDTOs = merchantConverter.toDTOList(merchants);

            log.info("Feign调用：获取所有商家成功，共{}条记录", merchantDTOs.size());
            return Result.success(merchantDTOs);
        } catch (Exception e) {
            log.error("Feign调用：获取所有商家失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取所有商家失败: " + e.getMessage());
        }
    }

    /**
     * 更新商家信息
     *
     * @param id       商家ID
     * @param merchant 商家信息
     * @return 更新后的商家信息
     */
    @Override
    public Result<MerchantDTO> updateMerchant(Long id, MerchantDTO merchant) {
        try {
            log.info("Feign调用：更新商家信息，商家ID: {}", id);

            Merchant existingMerchant = merchantService.getById(id);
            if (existingMerchant == null) {
                log.warn("商家不存在，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商家不存在");
            }

            Merchant merchantEntity = merchantConverter.toEntity(merchant);
            merchantEntity.setId(id);
            // 不能更新用户名和密码
            merchantEntity.setUsername(null);
            merchantEntity.setPassword(null);

            boolean updated = merchantService.updateById(merchantEntity);
            if (updated) {
                Merchant updatedMerchant = merchantService.getById(id);
                MerchantDTO updatedMerchantDTO = merchantConverter.toDTO(updatedMerchant);
                log.info("Feign调用：更新商家信息成功，商家ID: {}", id);
                return Result.success(updatedMerchantDTO);
            } else {
                log.error("Feign调用：更新商家信息失败，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新商家信息失败");
            }
        } catch (Exception e) {
            log.error("Feign调用：更新商家信息失败，商家ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新商家信息失败: " + e.getMessage());
        }
    }

    /**
     * 审核通过商家
     *
     * @param id 商家ID
     * @return 审核结果
     */
    @Override
    public Result<MerchantDTO> approveMerchant(Long id) {
        try {
            log.info("Feign调用：审核通过商家，商家ID: {}", id);

            Merchant merchant = merchantService.getById(id);
            if (merchant == null) {
                log.warn("商家不存在，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商家不存在");
            }

            // 设置商家状态为启用(1)
            merchant.setStatus(1);
            boolean updated = merchantService.updateById(merchant);

            if (updated) {
                MerchantDTO merchantDTO = merchantConverter.toDTO(merchant);
                log.info("Feign调用：审核通过商家成功，商家ID: {}", id);
                return Result.success(merchantDTO);
            } else {
                log.error("Feign调用：审核通过商家失败，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核通过商家失败");
            }
        } catch (Exception e) {
            log.error("Feign调用：审核通过商家失败，商家ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核通过商家失败: " + e.getMessage());
        }
    }

    /**
     * 拒绝商家申请
     *
     * @param id 商家ID
     * @return 审核结果
     */
    @Override
    public Result<MerchantDTO> rejectMerchant(Long id) {
        try {
            log.info("Feign调用：拒绝商家申请，商家ID: {}", id);

            Merchant merchant = merchantService.getById(id);
            if (merchant == null) {
                log.warn("商家不存在，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商家不存在");
            }

            // 设置商家状态为禁用(0)
            merchant.setStatus(0);
            boolean updated = merchantService.updateById(merchant);

            if (updated) {
                MerchantDTO merchantDTO = merchantConverter.toDTO(merchant);
                log.info("Feign调用：拒绝商家申请成功，商家ID: {}", id);
                return Result.success(merchantDTO);
            } else {
                log.error("Feign调用：拒绝商家申请失败，商家ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "拒绝商家申请失败");
            }
        } catch (Exception e) {
            log.error("Feign调用：拒绝商家申请失败，商家ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "拒绝商家申请失败: " + e.getMessage());
        }
    }

    /**
     * 创建店铺
     *
     * @param shop 店铺信息
     * @return 创建的店铺信息
     */
    @Override
    public Result<MerchantShopDTO> createShop(MerchantShopDTO shop) {
        try {
            log.info("Feign调用：创建店铺，店铺名称: {}", shop.getShopName());

            MerchantShop shopEntity = merchantConverter.toShopEntity(shop);
            boolean saved = merchantShopService.save(shopEntity);

            if (saved) {
                MerchantShopDTO savedShopDTO = merchantConverter.toShopDTO(shopEntity);
                log.info("Feign调用：创建店铺成功，店铺ID: {}", shopEntity.getId());
                return Result.success(savedShopDTO);
            } else {
                log.error("Feign调用：创建店铺失败");
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建店铺失败");
            }
        } catch (Exception e) {
            log.error("Feign调用：创建店铺失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建店铺失败: " + e.getMessage());
        }
    }

    /**
     * 根据商家ID获取店铺列表
     *
     * @param merchantId 商家ID
     * @return 店铺列表
     */
    @Override
    public Result<List<MerchantShopDTO>> getShopsByMerchantId(Long merchantId) {
        try {
            log.info("Feign调用：根据商家ID获取店铺列表，商家ID: {}", merchantId);

            LambdaQueryWrapper<MerchantShop> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MerchantShop::getMerchantId, merchantId);
            List<MerchantShop> shops = merchantShopService.list(queryWrapper);
            List<MerchantShopDTO> shopDTOs = merchantConverter.toShopDTOList(shops);

            log.info("Feign调用：根据商家ID获取店铺列表成功，商家ID: {}，共{}条记录", merchantId, shopDTOs.size());
            return Result.success(shopDTOs);
        } catch (Exception e) {
            log.error("Feign调用：根据商家ID获取店铺列表失败，商家ID: {}", merchantId, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取店铺列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取店铺详情
     *
     * @param id 店铺ID
     * @return 店铺详情
     */
    @Override
    public Result<MerchantShopDTO> getShopById(Long id) {
        try {
            log.info("Feign调用：获取店铺详情，店铺ID: {}", id);

            MerchantShop shop = merchantShopService.getById(id);
            if (shop == null) {
                log.warn("店铺不存在，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "店铺不存在");
            }

            MerchantShopDTO shopDTO = merchantConverter.toShopDTO(shop);
            log.info("Feign调用：获取店铺详情成功，店铺ID: {}", id);
            return Result.success(shopDTO);
        } catch (Exception e) {
            log.error("Feign调用：获取店铺详情失败，店铺ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取店铺详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新店铺信息
     *
     * @param id   店铺ID
     * @param shop 店铺信息
     * @return 更新后的店铺信息
     */
    @Override
    public Result<MerchantShopDTO> updateShop(Long id, MerchantShopDTO shop) {
        try {
            log.info("Feign调用：更新店铺信息，店铺ID: {}", id);

            MerchantShop existingShop = merchantShopService.getById(id);
            if (existingShop == null) {
                log.warn("店铺不存在，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "店铺不存在");
            }

            MerchantShop shopEntity = merchantConverter.toShopEntity(shop);
            shopEntity.setId(id);
            shopEntity.setMerchantId(existingShop.getMerchantId()); // 保持商家ID不变

            boolean updated = merchantShopService.updateById(shopEntity);
            if (updated) {
                MerchantShop updatedShop = merchantShopService.getById(id);
                MerchantShopDTO updatedShopDTO = merchantConverter.toShopDTO(updatedShop);
                log.info("Feign调用：更新店铺信息成功，店铺ID: {}", id);
                return Result.success(updatedShopDTO);
            } else {
                log.error("Feign调用：更新店铺信息失败，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新店铺信息失败");
            }
        } catch (Exception e) {
            log.error("Feign调用：更新店铺信息失败，店铺ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新店铺信息失败: " + e.getMessage());
        }
    }

    /**
     * 审核通过店铺
     *
     * @param id 店铺ID
     * @return 审核结果
     */
    @Override
    public Result<MerchantShopDTO> approveShop(Long id) {
        try {
            log.info("Feign调用：审核通过店铺，店铺ID: {}", id);

            MerchantShop shop = merchantShopService.getById(id);
            if (shop == null) {
                log.warn("店铺不存在，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "店铺不存在");
            }

            // 设置店铺状态为营业(1)
            shop.setStatus(1);
            boolean updated = merchantShopService.updateById(shop);

            if (updated) {
                MerchantShopDTO shopDTO = merchantConverter.toShopDTO(shop);
                log.info("Feign调用：审核通过店铺成功，店铺ID: {}", id);
                return Result.success(shopDTO);
            } else {
                log.error("Feign调用：审核通过店铺失败，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核通过店铺失败");
            }
        } catch (Exception e) {
            log.error("Feign调用：审核通过店铺失败，店铺ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核通过店铺失败: " + e.getMessage());
        }
    }

    /**
     * 拒绝店铺申请
     *
     * @param id 店铺ID
     * @return 审核结果
     */
    @Override
    public Result<MerchantShopDTO> rejectShop(Long id) {
        try {
            log.info("Feign调用：拒绝店铺申请，店铺ID: {}", id);

            MerchantShop shop = merchantShopService.getById(id);
            if (shop == null) {
                log.warn("店铺不存在，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "店铺不存在");
            }

            // 设置店铺状态为关闭(0)
            shop.setStatus(0);
            boolean updated = merchantShopService.updateById(shop);

            if (updated) {
                MerchantShopDTO shopDTO = merchantConverter.toShopDTO(shop);
                log.info("Feign调用：拒绝店铺申请成功，店铺ID: {}", id);
                return Result.success(shopDTO);
            } else {
                log.error("Feign调用：拒绝店铺申请失败，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "拒绝店铺申请失败");
            }
        } catch (Exception e) {
            log.error("Feign调用：拒绝店铺申请失败，店铺ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "拒绝店铺申请失败: " + e.getMessage());
        }
    }

    /**
     * 删除店铺
     *
     * @param id 店铺ID
     * @return 操作结果
     */
    @Override
    public Result<Void> deleteShop(Long id) {
        try {
            log.info("Feign调用：删除店铺，店铺ID: {}", id);

            MerchantShop shop = merchantShopService.getById(id);
            if (shop == null) {
                log.warn("店铺不存在，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "店铺不存在");
            }

            boolean removed = merchantShopService.removeById(id);
            if (removed) {
                log.info("Feign调用：删除店铺成功，店铺ID: {}", id);
                return Result.success();
            } else {
                log.error("Feign调用：删除店铺失败，店铺ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除店铺失败");
            }
        } catch (Exception e) {
            log.error("Feign调用：删除店铺失败，店铺ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除店铺失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有店铺
     *
     * @return 店铺列表
     */
    @Override
    public Result<List<MerchantShopDTO>> getAllShops() {
        try {
            log.info("Feign调用：获取所有店铺");

            List<MerchantShop> shops = merchantShopService.list();
            List<MerchantShopDTO> shopDTOs = merchantConverter.toShopDTOList(shops);

            log.info("Feign调用：获取所有店铺成功，共{}条记录", shopDTOs.size());
            return Result.success(shopDTOs);
        } catch (Exception e) {
            log.error("Feign调用：获取所有店铺失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取所有店铺失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有待审核的认证信息
     *
     * @return 认证信息列表
     */
    @Override
    public Result<List<MerchantAuthDTO>> getPendingAuths() {
        try {
            log.info("Feign调用：获取所有待审核的认证信息");

            LambdaQueryWrapper<MerchantAuth> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MerchantAuth::getAuthStatus, 0); // 0-待审核
            List<MerchantAuth> auths = merchantAuthService.list(queryWrapper);
            List<MerchantAuthDTO> authDTOs = merchantConverter.toAuthDTOList(auths);

            log.info("Feign调用：获取所有待审核的认证信息成功，共{}条记录", authDTOs.size());
            return Result.success(authDTOs);
        } catch (Exception e) {
            log.error("Feign调用：获取所有待审核的认证信息失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取所有待审核的认证信息失败: " + e.getMessage());
        }
    }

    /**
     * 审核商家认证信息
     *
     * @param id      认证信息ID
     * @param authDTO 审核信息
     * @return 审核结果
     */
    @Override
    public Result<MerchantAuthDTO> reviewMerchantAuth(Long id, MerchantAuthDTO authDTO) {
        try {
            log.info("Feign调用：审核商家认证信息，认证ID: {}", id);

            MerchantAuth existingAuth = merchantAuthService.getById(id);
            if (existingAuth == null) {
                log.warn("认证信息不存在，认证ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "认证信息不存在");
            }

            MerchantAuth auth = merchantConverter.toAuthEntity(authDTO);
            auth.setId(id);
            // 只能更新状态和备注
            existingAuth.setAuthStatus(auth.getAuthStatus());
            existingAuth.setAuthRemark(auth.getAuthRemark());

            boolean updated = merchantAuthService.updateById(existingAuth);
            if (updated) {
                MerchantAuth updatedAuth = merchantAuthService.getById(id);
                MerchantAuthDTO updatedAuthDTO = merchantConverter.toAuthDTO(updatedAuth);
                log.info("Feign调用：审核商家认证信息成功，认证ID: {}，状态: {}", id, authDTO.getAuthStatus());
                return Result.success(updatedAuthDTO);
            } else {
                log.error("Feign调用：审核商家认证信息失败，认证ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核商家认证信息失败");
            }
        } catch (Exception e) {
            log.error("Feign调用：审核商家认证信息失败，认证ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "审核商家认证信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有认证信息
     *
     * @return 认证信息列表
     */
    @Override
    public Result<List<MerchantAuthDTO>> getAllAuths() {
        try {
            log.info("Feign调用：获取所有认证信息");

            List<MerchantAuth> auths = merchantAuthService.list();
            List<MerchantAuthDTO> authDTOs = merchantConverter.toAuthDTOList(auths);

            log.info("Feign调用：获取所有认证信息成功，共{}条记录", authDTOs.size());
            return Result.success(authDTOs);
        } catch (Exception e) {
            log.error("Feign调用：获取所有认证信息失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取所有认证信息失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取认证信息
     *
     * @param id 认证信息ID
     * @return 认证信息
     */
    @Override
    public Result<MerchantAuthDTO> getAuthById(Long id) {
        try {
            log.info("Feign调用：根据ID获取认证信息，认证ID: {}", id);

            MerchantAuth auth = merchantAuthService.getById(id);
            if (auth == null) {
                log.warn("认证信息不存在，认证ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "认证信息不存在");
            }

            MerchantAuthDTO authDTO = merchantConverter.toAuthDTO(auth);
            log.info("Feign调用：根据ID获取认证信息成功，认证ID: {}", id);
            return Result.success(authDTO);
        } catch (Exception e) {
            log.error("Feign调用：根据ID获取认证信息失败，认证ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "获取认证信息失败: " + e.getMessage());
        }
    }
}