package com.cloud.api.merchant;

import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.merchant.MerchantAuthDTO;
import com.cloud.common.domain.dto.merchant.MerchantDTO;
import com.cloud.common.domain.dto.merchant.MerchantShopDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "merchant-service")
public interface MerchantFeignClient {

    /**
     * 获取商家信息
     *
     * @param id 商家ID
     * @return 商家信息
     */
    @GetMapping("/merchants/{id}")
    Result<MerchantDTO> getMerchantById(@PathVariable("id") Long id);

    /**
     * 获取所有商家
     *
     * @return 商家列表
     */
    @GetMapping("/merchants")
    Result<List<MerchantDTO>> getAllMerchants();

    /**
     * 更新商家信息
     *
     * @param id       商家ID
     * @param merchant 商家信息
     * @return 更新后的商家信息
     */
    @PutMapping("/merchants/{id}")
    Result<MerchantDTO> updateMerchant(@PathVariable("id") Long id, @RequestBody MerchantDTO merchant);

    /**
     * 审核通过商家
     *
     * @param id 商家ID
     * @return 审核结果
     */
    @PutMapping("/merchants/{id}/approve")
    Result<MerchantDTO> approveMerchant(@PathVariable("id") Long id);

    /**
     * 拒绝商家申请
     *
     * @param id 商家ID
     * @return 审核结果
     */
    @PutMapping("/merchants/{id}/reject")
    Result<MerchantDTO> rejectMerchant(@PathVariable("id") Long id);

    /**
     * 创建店铺
     *
     * @param shop 店铺信息
     * @return 创建的店铺信息
     */
    @PostMapping("/shops")
    Result<MerchantShopDTO> createShop(@RequestBody MerchantShopDTO shop);

    /**
     * 获取商家的所有店铺
     *
     * @param merchantId 商家ID
     * @return 店铺列表
     */
    @GetMapping("/shops/merchant/{merchantId}")
    Result<List<MerchantShopDTO>> getShopsByMerchantId(@PathVariable("merchantId") Long merchantId);

    /**
     * 获取店铺详情
     *
     * @param id 店铺ID
     * @return 店铺详情
     */
    @GetMapping("/shops/{id}")
    Result<MerchantShopDTO> getShopById(@PathVariable("id") Long id);

    /**
     * 更新店铺信息
     *
     * @param id   店铺ID
     * @param shop 店铺信息
     * @return 更新后的店铺信息
     */
    @PutMapping("/shops/{id}")
    Result<MerchantShopDTO> updateShop(@PathVariable("id") Long id, @RequestBody MerchantShopDTO shop);

    /**
     * 审核通过店铺
     *
     * @param id 店铺ID
     * @return 审核结果
     */
    @PutMapping("/shops/{id}/approve")
    Result<MerchantShopDTO> approveShop(@PathVariable("id") Long id);

    /**
     * 拒绝店铺申请
     *
     * @param id 店铺ID
     * @return 审核结果
     */
    @PutMapping("/shops/{id}/reject")
    Result<MerchantShopDTO> rejectShop(@PathVariable("id") Long id);

    /**
     * 删除店铺
     *
     * @param id 店铺ID
     * @return 操作结果
     */
    @DeleteMapping("/shops/{id}")
    Result<Void> deleteShop(@PathVariable("id") Long id);

    /**
     * 获取所有店铺
     *
     * @return 店铺列表
     */
    @GetMapping("/shops")
    Result<List<MerchantShopDTO>> getAllShops();

    /**
     * 获取所有待审核的认证信息
     *
     * @return 认证信息列表
     */
    @GetMapping("/merchant-auth/pending")
    Result<List<MerchantAuthDTO>> getPendingAuths();

    /**
     * 审核商家认证信息
     *
     * @param id      认证信息ID
     * @param authDTO 审核信息
     * @return 审核结果
     */
    @PutMapping("/merchant-auth/review/{id}")
    Result<MerchantAuthDTO> reviewMerchantAuth(@PathVariable("id") Long id, @RequestBody MerchantAuthDTO authDTO);

    /**
     * 获取所有认证信息
     *
     * @return 认证信息列表
     */
    @GetMapping("/merchant-auth")
    Result<List<MerchantAuthDTO>> getAllAuths();

    /**
     * 根据ID获取认证信息
     *
     * @param id 认证信息ID
     * @return 认证信息
     */
    @GetMapping("/merchant-auth/{id}")
    Result<MerchantAuthDTO> getAuthById(@PathVariable("id") Long id);
}