package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.UserAddressDTO;
import com.cloud.common.domain.dto.user.UserAddressRequestDTO;
import com.cloud.common.domain.vo.UserAddressVO;
import com.cloud.user.module.entity.UserAddress;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;




@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        unmappedSourcePolicy = ReportingPolicy.IGNORE  
)
public interface UserAddressConverter {

    UserAddressConverter INSTANCE = org.mapstruct.factory.Mappers.getMapper(UserAddressConverter.class);

    UserAddress toEntity(UserAddressDTO userAddressDTO);

    UserAddressDTO toDTO(UserAddress userAddress);

    





    UserAddress toEntity(UserAddressRequestDTO userAddressRequestDTO);

    





    UserAddressVO toVO(UserAddress userAddress);

    





    List<UserAddressVO> toVOList(List<UserAddress> userAddresses);

    





    List<UserAddressDTO> toDTOList(List<UserAddress> userAddresses);
}
