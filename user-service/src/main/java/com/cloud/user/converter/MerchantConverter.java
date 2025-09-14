package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.domain.vo.MerchantVO;
import com.cloud.user.module.entity.Merchant;
import com.cloud.user.module.entity.MerchantAuth;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 商家相关实体与DTO转换器
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface MerchantConverter {

    MerchantConverter INSTANCE = Mappers.getMapper(MerchantConverter.class);

    /**
     * 转换商家实体为DTO
     *
     * @param merchant 商家实体
     * @return 商家DTO
     */
    @Mapping(target = "id", source = "id")
    MerchantDTO toDTO(Merchant merchant);

    /**
     * 转换商家DTO为实体
     *
     * @param merchantDTO 商家DTO
     * @return 商家实体
     */
    Merchant toEntity(MerchantDTO merchantDTO);

    /**
     * 转换商家实体列表为DTO列表
     *
     * @param merchants 商家实体列表
     * @return 商家DTO列表
     */
    List<MerchantDTO> toDTOList(List<Merchant> merchants);

    /**
     * 转换认证实体为DTO
     *
     * @param merchantAuth 认证实体
     * @return 认证DTO
     */
    @Mapping(target = "id", source = "id")
    MerchantAuthDTO toAuthDTO(MerchantAuth merchantAuth);

    /**
     * 转换认证DTO为实体
     *
     * @param merchantAuthDTO 认证DTO
     * @return 认证实体
     */
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MerchantAuth toAuthEntity(MerchantAuthDTO merchantAuthDTO);

    /**
     * 转换认证实体列表为DTO列表
     *
     * @param merchantAuths 认证实体列表
     * @return 认证DTO列表
     */
    List<MerchantAuthDTO> toAuthDTOList(List<MerchantAuth> merchantAuths);

    /**
     * 转换商家实体为VO
     *
     * @param merchant 商家实体
     * @return 商家VO
     */
    MerchantVO toVO(Merchant merchant);

    /**
     * 转换商家实体列表为VO列表
     *
     * @param merchants 商家实体列表
     * @return 商家VO列表
     */
    List<MerchantVO> toVOList(List<Merchant> merchants);
}