package com.cloud.user.converter;

import com.cloud.common.domain.dto.UserDTO;
import com.cloud.user.module.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserConverter {

    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    UserDTO toDTO(User user);

    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    User toEntity(UserDTO userDTO);
}
