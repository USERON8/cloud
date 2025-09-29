package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantAuthRequestDTO;
import com.cloud.common.domain.vo.user.MerchantAuthVO;
import com.cloud.user.module.entity.MerchantAuth;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 商家认证转换器
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // 忽略未映射目标属性
        unmappedSourcePolicy = ReportingPolicy.IGNORE  // 忽略未映射源属性
)
public interface MerchantAuthConverter {

    MerchantAuthConverter INSTANCE = org.mapstruct.factory.Mappers.getMapper(MerchantAuthConverter.class);

    /**
     * 转换商家认证实体为DTO
     *
     * @param merchantAuth 商家认证实体
     * @return 商家认证DTO
     */
    MerchantAuthDTO toDTO(MerchantAuth merchantAuth);

    /**
     * 转换商家认证DTO为实体
     *
     * @param merchantAuthDTO 商家认证DTO
     * @return 商家认证实体
     */
    MerchantAuth toEntity(MerchantAuthDTO merchantAuthDTO);

    /**
     * 转换商家认证请求DTO为实体
     *
     * @param merchantAuthRequestDTO 商家认证请求DTO
     * @return 商家认证实体
     */
    MerchantAuth toEntity(MerchantAuthRequestDTO merchantAuthRequestDTO);

    /**
     * 转换商家认证实体为VO
     *
     * @param merchantAuth 商家认证实体
     * @return 商家认证VO
     */
    MerchantAuthVO toVO(MerchantAuth merchantAuth);

    /**
     * 转换商家认证实体列表为VO列表
     *
     * @param merchantAuths 商家认证实体列表
     * @return 商家认证VO列表
     */
    List<MerchantAuthVO> toVOList(List<MerchantAuth> merchantAuths);

    /**
     * 转换商家认证实体列表为DTO列表
     *
     * @param merchantAuths 商家认证实体列表
     * @return 商家认证DTO列表
     */
    List<MerchantAuthDTO> toDTOList(List<MerchantAuth> merchantAuths);
}