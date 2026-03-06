package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.vo.user.UserVO;
import com.cloud.user.module.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface UserConverter {

    UserConverter INSTANCE = org.mapstruct.factory.Mappers.getMapper(UserConverter.class);

    UserDTO toDTO(User user);

    User toEntity(UserDTO userDTO);

    List<UserDTO> toDTOList(List<User> users);

    UserVO toVO(User user);

    List<UserVO> toVOList(List<User> users);

    UserVO dtoToVO(UserDTO userDTO);

    List<UserVO> dtoToVOList(List<UserDTO> userDTOs);
}
