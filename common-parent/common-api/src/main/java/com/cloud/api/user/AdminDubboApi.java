package com.cloud.api.user;

import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;

import java.util.List;

public interface AdminDubboApi {

    AdminDTO findById(Long id);

    List<AdminDTO> findAll();

    AdminDTO create(AdminUpsertRequestDTO requestDTO);

    AdminDTO update(Long id, AdminUpsertRequestDTO requestDTO);

    Boolean delete(Long id);
}

