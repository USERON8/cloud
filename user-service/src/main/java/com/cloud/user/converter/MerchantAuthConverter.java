package com.cloud.user.converter;

import com.cloud.common.domain.dto.MerchantAuthDTO;
import com.cloud.user.module.entity.MerchantAuth;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * 商家认证转换器
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface MerchantAuthConverter {
    
    /**
     * 将实体转换为DTO
     *
     * @param merchantAuth 实体
     * @return DTO
     */
    MerchantAuthDTO toDTO(MerchantAuth merchantAuth);

    /**
     * 将DTO转换为实体
     *
     * @param merchantAuthDTO DTO
     * @return 实体
     */
    MerchantAuth toEntity(MerchantAuthDTO merchantAuthDTO);
}