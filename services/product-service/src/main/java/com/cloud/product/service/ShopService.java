package com.cloud.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.result.PageResult;
import com.cloud.product.module.dto.ShopPageDTO;
import com.cloud.product.module.dto.ShopRequestDTO;
import com.cloud.product.module.entity.Shop;
import com.cloud.product.module.vo.ShopVO;

import java.util.List;









public interface ShopService extends IService<Shop> {

    

    





    Long createShop(ShopRequestDTO requestDTO);

    






    Boolean updateShop(Long id, ShopRequestDTO requestDTO);

    





    Boolean deleteShop(Long id);

    





    Boolean batchDeleteShops(List<Long> ids);

    

    





    ShopVO getShopById(Long id);

    





    List<ShopVO> getShopsByIds(List<Long> ids);

    





    PageResult<ShopVO> getShopsPage(ShopPageDTO pageDTO);

    






    List<ShopVO> getShopsByMerchantId(Long merchantId, Integer status);

    






    List<ShopVO> searchShopsByName(String shopName, Integer status);

    

    





    Boolean enableShop(Long id);

    





    Boolean disableShop(Long id);

    





    Boolean batchEnableShops(List<Long> ids);

    





    Boolean batchDisableShops(List<Long> ids);

    

    




    Long getTotalShopCount();

    




    Long getEnabledShopCount();

    




    Long getDisabledShopCount();

    





    Long getShopCountByMerchantId(Long merchantId);

    

    






    Boolean hasPermission(Long merchantId, Long shopId);

    

    




    void evictShopCache(Long id);

    


    void evictAllShopCache();

    




    void warmupShopCache(List<Long> ids);
}
