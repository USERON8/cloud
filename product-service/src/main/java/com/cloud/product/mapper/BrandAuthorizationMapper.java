package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.BrandAuthorization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 品牌授权 Mapper
 *
 * @author what's up
 */
@Mapper
public interface BrandAuthorizationMapper extends BaseMapper<BrandAuthorization> {
    /**
     * 根据品牌ID查询授权记录
     *
     * @param brandId 品牌ID
     * @return 授权记录列表
     */
    List<BrandAuthorization> selectByBrandId(@Param("brandId") Long brandId);

    /**
     * 根据商家ID查询授权记录
     *
     * @param merchantId 商家ID
     * @return 授权记录列表
     */
    List<BrandAuthorization> selectByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * 检查商家是否有品牌授权
     *
     * @param brandId    品牌ID
     * @param merchantId 商家ID
     * @return 授权记录
     */
    BrandAuthorization selectByBrandIdAndMerchantId(@Param("brandId") Long brandId,
                                                    @Param("merchantId") Long merchantId);

    /**
     * 查询即将过期的授权(30天内)
     *
     * @param expiryDate 过期日期
     * @return 授权记录列表
     */
    List<BrandAuthorization> selectExpiringSoon(@Param("expiryDate") LocalDateTime expiryDate);
}
