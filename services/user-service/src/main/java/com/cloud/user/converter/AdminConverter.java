package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.vo.user.AdminVO;
import com.cloud.user.module.entity.Admin;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface AdminConverter {

  AdminDTO toDTO(Admin admin);

  AdminVO toVO(Admin admin);

  List<AdminVO> toVOList(List<Admin> admins);

  List<AdminDTO> toDTOList(List<Admin> admins);
}
