package com.cloud.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.product.module.entity.ProductAudit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;






@Mapper
public interface ProductAuditMapper extends BaseMapper<ProductAudit> {
    





    List<ProductAudit> selectByProductId(@Param("productId") Long productId);

    







    List<ProductAudit> selectByMerchantId(@Param("merchantId") Long merchantId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    







    List<ProductAudit> selectByAuditStatus(@Param("auditStatus") String auditStatus,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    




    int countPendingAudits();
}
