package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.UserAddressDTO;
import com.cloud.user.module.entity.UserAddress;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // 忽略未映射目标属性
        unmappedSourcePolicy = ReportingPolicy.IGNORE  // 忽略未映射源属性
)
public interface UserAddressConverter {
    UserAddressConverter INSTANCE = Mappers.getMapper(UserAddressConverter.class);

    UserAddress toEntity(UserAddressDTO userAddressDTO);

    UserAddressDTO toDTO(UserAddress userAddress);
}
