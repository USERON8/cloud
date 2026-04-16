package com.cloud.api.user;

import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.common.domain.vo.user.AdminPageVO;

public interface AdminGovernanceDubboApi {

  AdminPageVO getAdminsPage(Integer page, Integer size);

  AdminDTO getAdminById(Long id);

  AdminDTO createAdmin(AdminUpsertRequestDTO requestDTO);

  Boolean updateAdmin(Long id, AdminUpsertRequestDTO requestDTO);

  Boolean deleteAdmin(Long id);

  Boolean updateAdminStatus(Long id, Integer status);

  String resetPassword(Long id);
}
