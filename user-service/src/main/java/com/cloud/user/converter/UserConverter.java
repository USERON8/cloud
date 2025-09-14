package com.cloud.user.converter;

import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.vo.UserVO;
import com.cloud.user.module.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 用户转换器
 *
 * @author what's up
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // 忽略未映射目标属性
        unmappedSourcePolicy = ReportingPolicy.IGNORE  // 忽略未映射源属性
)
public interface UserConverter {
    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    /**
     * 转换用户实体为DTO
     *
     * @param user 用户实体
     * @return 用户DTO
     */
    UserDTO toDTO(User user);

    /**
     * 转换用户DTO为实体
     *
     * @param userDTO 用户DTO
     * @return 用户实体
     */
    User toEntity(UserDTO userDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
        // 忽略createdAt和updatedAt字段，让MyBatis Plus自动填充
    User toEntity(RegisterRequestDTO registerRequestDTO);

    /**
     * 转换用户实体列表为DTO列表
     * 注意：密码字段不会被转换以确保安全
     *
     * @param users 用户实体列表
     * @return 用户DTO列表
     */
    List<UserDTO> toDTOList(List<User> users);


    /**
     * 转换用户实体为VO
     *
     * @param user 用户实体
     * @return 用户VO
     */
    UserVO toVO(User user);

    /**
     * 转换用户实体列表为VO列表
     *
     * @param users 用户实体列表
     * @return 用户VO列表
     */
    List<UserVO> toVOList(List<User> users);
}