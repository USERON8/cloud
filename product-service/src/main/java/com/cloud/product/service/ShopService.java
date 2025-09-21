package com.cloud.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.result.PageResult;
import com.cloud.product.module.dto.ShopPageDTO;
import com.cloud.product.module.dto.ShopRequestDTO;
import com.cloud.product.module.entity.Shop;
import com.cloud.product.module.vo.ShopVO;

import java.util.List;

/**
 * 店铺服务接口
 * 提供店铺相关的业务操作，包括CRUD、分页查询、状态管理等
 * 使用多级缓存提升性能，遵循用户服务标准
 *
 * @author what's up
 * @since 1.0.0
 */
public interface ShopService extends IService<Shop> {

    // ================= 基础CRUD操作 =================

    /**
     * 创建店铺
     *
     * @param requestDTO 店铺创建请求DTO
     * @return 店铺ID
     */
    Long createShop(ShopRequestDTO requestDTO);

    /**
     * 更新店铺
     *
     * @param id         店铺ID
     * @param requestDTO 店铺更新请求DTO
     * @return 是否更新成功
     */
    Boolean updateShop(Long id, ShopRequestDTO requestDTO);

    /**
     * 删除店铺
     *
     * @param id 店铺ID
     * @return 是否删除成功
     */
    Boolean deleteShop(Long id);

    /**
     * 批量删除店铺
     *
     * @param ids 店铺ID列表
     * @return 是否删除成功
     */
    Boolean batchDeleteShops(List<Long> ids);

    // ================= 查询操作 =================

    /**
     * 根据ID获取店铺详情
     *
     * @param id 店铺ID
     * @return 店铺VO
     */
    ShopVO getShopById(Long id);

    /**
     * 根据ID列表批量获取店铺
     *
     * @param ids 店铺ID列表
     * @return 店铺VO列表
     */
    List<ShopVO> getShopsByIds(List<Long> ids);

    /**
     * 分页查询店铺
     *
     * @param pageDTO 分页查询参数
     * @return 分页结果
     */
    PageResult<ShopVO> getShopsPage(ShopPageDTO pageDTO);

    /**
     * 根据商家ID获取店铺列表
     *
     * @param merchantId 商家ID
     * @param status     店铺状态，null表示查询所有状态
     * @return 店铺VO列表
     */
    List<ShopVO> getShopsByMerchantId(Long merchantId, Integer status);

    /**
     * 根据店铺名称模糊查询
     *
     * @param shopName 店铺名称关键字
     * @param status   店铺状态，null表示查询所有状态
     * @return 店铺VO列表
     */
    List<ShopVO> searchShopsByName(String shopName, Integer status);

    // ================= 状态管理 =================

    /**
     * 开启店铺（营业）
     *
     * @param id 店铺ID
     * @return 是否操作成功
     */
    Boolean enableShop(Long id);

    /**
     * 关闭店铺
     *
     * @param id 店铺ID
     * @return 是否操作成功
     */
    Boolean disableShop(Long id);

    /**
     * 批量开启店铺
     *
     * @param ids 店铺ID列表
     * @return 是否操作成功
     */
    Boolean batchEnableShops(List<Long> ids);

    /**
     * 批量关闭店铺
     *
     * @param ids 店铺ID列表
     * @return 是否操作成功
     */
    Boolean batchDisableShops(List<Long> ids);

    // ================= 统计分析 =================

    /**
     * 获取店铺总数
     *
     * @return 店铺总数
     */
    Long getTotalShopCount();

    /**
     * 获取营业店铺数量
     *
     * @return 营业店铺数量
     */
    Long getEnabledShopCount();

    /**
     * 获取关闭店铺数量
     *
     * @return 关闭店铺数量
     */
    Long getDisabledShopCount();

    /**
     * 根据商家统计店铺数量
     *
     * @param merchantId 商家ID
     * @return 该商家下的店铺数量
     */
    Long getShopCountByMerchantId(Long merchantId);

    // ================= 权限验证 =================

    /**
     * 检查商家是否有权限访问店铺
     *
     * @param merchantId 商家ID
     * @param shopId     店铺ID
     * @return 是否有权限
     */
    Boolean hasPermission(Long merchantId, Long shopId);

    // ================= 缓存管理 =================

    /**
     * 清除店铺缓存
     *
     * @param id 店铺ID
     */
    void evictShopCache(Long id);

    /**
     * 清除所有店铺缓存
     */
    void evictAllShopCache();

    /**
     * 预热店铺缓存
     *
     * @param ids 需要预热的店铺ID列表
     */
    void warmupShopCache(List<Long> ids);
}
