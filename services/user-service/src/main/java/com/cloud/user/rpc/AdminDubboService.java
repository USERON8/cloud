package com.cloud.user.rpc;

import com.cloud.api.user.AdminDubboApi;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.user.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService(interfaceClass = AdminDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class AdminDubboService implements AdminDubboApi {

    private final AdminService adminService;

    @Override
    public AdminDTO findById(Long id) {
        return adminService.getAdminById(id);
    }

    @Override
    public List<AdminDTO> findAll() {
        return adminService.listAdmins();
    }

    @Override
    public Long create(AdminUpsertRequestDTO requestDTO) {
        AdminDTO adminDTO = adminService.createAdmin(requestDTO);
        return adminDTO == null ? null : adminDTO.getId();
    }

    @Override
    public Boolean update(Long id, AdminUpsertRequestDTO requestDTO) {
        return adminService.updateAdmin(id, requestDTO);
    }

    @Override
    public Boolean delete(Long id) {
        return adminService.deleteAdmin(id);
    }
}

