package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.ProductAudit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品审核 Mapper
 *
 * @author what's up
 */
@Mapper
public interface ProductAuditMapper extends BaseMapper<ProductAudit> {
    /**
     * 根据商品ID查询审核记录
     *
     * @param productId 商品ID
     * @return 审核记录列表
     */
    List<ProductAudit> selectByProductId(@Param("productId") Long productId);

    /**
     * 根据商家ID查询审核记录
     *
     * @param merchantId 商家ID
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 审核记录列表
     */
    List<ProductAudit> selectByMerchantId(@Param("merchantId") Long merchantId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 根据审核状态查询审核记录
     *
     * @param auditStatus 审核状态
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @return 审核记录列表
     */
    List<ProductAudit> selectByAuditStatus(@Param("auditStatus") String auditStatus,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 查询待审核数量
     *
     * @return 待审核数量
     */
    int countPendingAudits();
}
