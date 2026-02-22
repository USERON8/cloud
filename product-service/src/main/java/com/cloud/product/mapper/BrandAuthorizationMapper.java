package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.BrandAuthorization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;






@Mapper
public interface BrandAuthorizationMapper extends BaseMapper<BrandAuthorization> {
    





    List<BrandAuthorization> selectByBrandId(@Param("brandId") Long brandId);

    





    List<BrandAuthorization> selectByMerchantId(@Param("merchantId") Long merchantId);

    






    BrandAuthorization selectByBrandIdAndMerchantId(@Param("brandId") Long brandId,
                                                    @Param("merchantId") Long merchantId);

    





    List<BrandAuthorization> selectExpiringSoon(@Param("expiryDate") LocalDateTime expiryDate);
}
