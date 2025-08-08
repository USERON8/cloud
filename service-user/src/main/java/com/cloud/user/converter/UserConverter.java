package com.cloud.user.converter;

import com.cloud.common.domain.dto.UserDTO;
import com.cloud.user.module.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // 忽略未映射目标属性
        unmappedSourcePolicy = ReportingPolicy.IGNORE  // 忽略未映射源属性
)
public interface UserConverter {

    UserDTO toDTO(User user);


    User toEntity(UserDTO userDTO);
}