package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.UserAddressDTO;
import com.cloud.common.domain.dto.user.UserAddressRequestDTO;
import com.cloud.common.domain.vo.UserAddressVO;
import com.cloud.user.module.entity.UserAddress;
import com.cloud.user.service.cache.TransactionalUserAddressCacheService;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UserAddressConverter {

  UserAddressConverter INSTANCE =
      org.mapstruct.factory.Mappers.getMapper(UserAddressConverter.class);

  UserAddress toEntity(UserAddressDTO userAddressDTO);

  UserAddressDTO toDTO(UserAddress userAddress);

  UserAddressDTO toDTO(TransactionalUserAddressCacheService.UserAddressCache cache);

  UserAddress toEntity(UserAddressRequestDTO userAddressRequestDTO);

  UserAddressVO toVO(UserAddress userAddress);

  UserAddressVO toVO(UserAddressDTO userAddressDTO);

  List<UserAddressVO> toVOList(List<UserAddress> userAddresses);

  List<UserAddressDTO> toDTOList(List<UserAddress> userAddresses);
}
