package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.common.domain.vo.user.AdminVO;
import com.cloud.user.module.entity.Admin;
import com.cloud.user.service.cache.TransactionalAdminCacheService;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface AdminConverter {

  AdminDTO toDTO(Admin admin);

  AdminDTO toDTO(TransactionalAdminCacheService.AdminCache cache);

  Admin toEntity(AdminUpsertRequestDTO requestDTO);

  AdminVO toVO(Admin admin);

  List<AdminVO> toVOList(List<Admin> admins);

  List<AdminDTO> toDTOList(List<Admin> admins);
}
