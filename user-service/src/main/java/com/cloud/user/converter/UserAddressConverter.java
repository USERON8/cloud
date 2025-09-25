package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.UserAddressDTO;
import com.cloud.common.domain.dto.user.UserAddressRequestDTO;
import com.cloud.common.domain.vo.UserAddressVO;
import com.cloud.user.module.entity.UserAddress;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 用户地址转换器
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // 忽略未映射目标属性
        unmappedSourcePolicy = ReportingPolicy.IGNORE  // 忽略未映射源属性
)
public interface UserAddressConverter {

    UserAddressConverter INSTANCE = org.mapstruct.factory.Mappers.getMapper(UserAddressConverter.class);

    UserAddress toEntity(UserAddressDTO userAddressDTO);

    UserAddressDTO toDTO(UserAddress userAddress);

    /**
     * 转换用户地址请求DTO为实体
     *
     * @param userAddressRequestDTO 用户地址请求DTO
     * @return 用户地址实体
     */
    UserAddress toEntity(UserAddressRequestDTO userAddressRequestDTO);

    /**
     * 转换用户地址实体为VO
     *
     * @param userAddress 用户地址实体
     * @return 用户地址VO
     */
    UserAddressVO toVO(UserAddress userAddress);

    /**
     * 转换用户地址实体列表为VO列表
     *
     * @param userAddresses 用户地址实体列表
     * @return 用户地址VO列表
     */
    List<UserAddressVO> toVOList(List<UserAddress> userAddresses);

    /**
     * 转换用户地址实体列表为DTO列表
     *
     * @param userAddresses 用户地址实体列表
     * @return 用户地址DTO列表
     */
    List<UserAddressDTO> toDTOList(List<UserAddress> userAddresses);
}