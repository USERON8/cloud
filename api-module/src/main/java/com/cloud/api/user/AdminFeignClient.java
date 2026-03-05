package com.cloud.api.user;

import com.cloud.common.domain.dto.user.AdminDTO;

import java.util.List;

public interface AdminFeignClient {

    AdminDTO findById(Long id);

    List<AdminDTO> findAll();

    AdminDTO create(AdminDTO adminDTO);

    AdminDTO update(Long id, AdminDTO adminDTO);

    Boolean delete(Long id);
}
