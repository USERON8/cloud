package com.cloud.user.converter;

import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.vo.user.UserVO;
import com.cloud.user.module.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
        
    User toEntity(RegisterRequestDTO registerRequestDTO);

    






    List<UserDTO> toDTOList(List<User> users);


    





    UserVO toVO(User user);

    





    List<UserVO> toVOList(List<User> users);

    





    UserVO dtoToVO(UserDTO userDTO);

    





    List<UserVO> dtoToVOList(List<UserDTO> userDTOs);

    






    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true) 
    @Mapping(target = "password", ignore = true) 
    @Mapping(target = "phone", ignore = true) 
    @Mapping(target = "userType", ignore = true) 
    @Mapping(target = "status", ignore = true) 
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "nickname", source = "name")
    @Mapping(target = "githubId", source = "githubId")
    @Mapping(target = "githubUsername", source = "login")
    @Mapping(target = "oauthProvider", constant = "github")
    @Mapping(target = "oauthProviderId", expression = "java(githubUserDTO.getGithubId().toString())")
    User fromGitHubUserDTO(com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO);
}
