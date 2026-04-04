package com.cloud.user.converter;

import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.common.domain.dto.user.MerchantUpsertRequestDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
import com.cloud.common.domain.dto.user.UserUpsertRequestDTO;
import com.cloud.user.module.entity.Admin;
import com.cloud.user.module.entity.Merchant;
import com.cloud.user.module.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface AuthPrincipalConverter {

  AuthPrincipalDTO toDTO(User user);

  AuthPrincipalDTO toDTO(Admin admin);

  AuthPrincipalDTO toDTO(Merchant merchant);

  AuthPrincipalDTO toDTO(UserUpsertRequestDTO requestDTO);

  AuthPrincipalDTO toDTO(UserProfileUpsertDTO profileUpsertDTO);

  AuthPrincipalDTO toDTO(AdminUpsertRequestDTO requestDTO);

  AuthPrincipalDTO toDTO(MerchantUpsertRequestDTO requestDTO);
}
