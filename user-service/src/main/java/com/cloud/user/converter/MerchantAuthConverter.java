package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantAuthRequestDTO;
import com.cloud.common.domain.vo.user.MerchantAuthVO;
import com.cloud.user.module.entity.MerchantAuth;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;




@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        unmappedSourcePolicy = ReportingPolicy.IGNORE  
)
public interface MerchantAuthConverter {

    MerchantAuthConverter INSTANCE = org.mapstruct.factory.Mappers.getMapper(MerchantAuthConverter.class);

    





    MerchantAuthDTO toDTO(MerchantAuth merchantAuth);

    





    MerchantAuth toEntity(MerchantAuthDTO merchantAuthDTO);

    





    MerchantAuth toEntity(MerchantAuthRequestDTO merchantAuthRequestDTO);

    





    MerchantAuthVO toVO(MerchantAuth merchantAuth);

    





    List<MerchantAuthVO> toVOList(List<MerchantAuth> merchantAuths);

    





    List<MerchantAuthDTO> toDTOList(List<MerchantAuth> merchantAuths);
}
