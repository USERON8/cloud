package com.cloud.user.converter;

import com.cloud.common.domain.dto.UserDTO;
import com.cloud.user.module.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserConverter {

    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);


    UserDTO toDTO(User user);


    User toEntity(UserDTO userDTO);
}