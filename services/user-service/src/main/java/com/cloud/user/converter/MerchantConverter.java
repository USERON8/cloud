package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.domain.vo.MerchantVO;
import com.cloud.user.module.entity.Merchant;
import com.cloud.user.module.entity.MerchantAuth;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;




@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface MerchantConverter {

    MerchantConverter INSTANCE = org.mapstruct.factory.Mappers.getMapper(MerchantConverter.class);


    





    @Mapping(target = "id", source = "id")
    MerchantDTO toDTO(Merchant merchant);

    





    Merchant toEntity(MerchantDTO merchantDTO);

    





    List<MerchantDTO> toDTOList(List<Merchant> merchants);

    





    @Mapping(target = "id", source = "id")
    MerchantAuthDTO toAuthDTO(MerchantAuth merchantAuth);

    





    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MerchantAuth toAuthEntity(MerchantAuthDTO merchantAuthDTO);

    





    List<MerchantAuthDTO> toAuthDTOList(List<MerchantAuth> merchantAuths);

    





    MerchantVO toVO(Merchant merchant);

    





    List<MerchantVO> toVOList(List<Merchant> merchants);
}
