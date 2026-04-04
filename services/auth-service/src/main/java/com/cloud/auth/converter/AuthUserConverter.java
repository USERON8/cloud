package com.cloud.auth.converter;

import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.oauth.GitHubUserDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserProfileDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface AuthUserConverter {

  AuthPrincipalDTO toPrincipalDTO(RegisterRequestDTO requestDTO);

  @Mapping(target = "nickname", expression = "java(gitHubUserDTO.getDisplayName())")
  AuthPrincipalDTO toPrincipalDTO(GitHubUserDTO gitHubUserDTO);

  UserDTO toUserDTO(UserProfileDTO profileDTO);

  UserProfileDTO toProfileDTO(UserProfileUpsertDTO profileUpsertDTO);

  @Mapping(target = "id", source = "principal.id")
  @Mapping(target = "username", source = "principal.username")
  @Mapping(target = "phone", source = "request.phone")
  @Mapping(target = "nickname", source = "request.nickname")
  @Mapping(target = "status", source = "principal.status")
  UserProfileUpsertDTO toProfileUpsertDTO(AuthPrincipalDTO principal, RegisterRequestDTO request);

  @Mapping(target = "id", source = "principal.id")
  @Mapping(target = "username", source = "principal.username")
  @Mapping(target = "nickname", expression = "java(gitHubUserDTO.getDisplayName())")
  @Mapping(target = "email", source = "gitHubUserDTO.email")
  @Mapping(target = "avatarUrl", source = "gitHubUserDTO.avatarUrl")
  @Mapping(target = "status", source = "principal.status")
  UserProfileUpsertDTO toProfileUpsertDTO(AuthPrincipalDTO principal, GitHubUserDTO gitHubUserDTO);
}
