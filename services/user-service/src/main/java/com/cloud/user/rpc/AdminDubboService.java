package com.cloud.user.rpc;

import com.cloud.api.user.AdminDubboApi;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.user.converter.AdminConverter;
import com.cloud.user.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService(interfaceClass = AdminDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class AdminDubboService implements AdminDubboApi {

    private final AdminService adminService;
    private final AdminConverter adminConverter;

    @Override
    public AdminDTO findById(Long id) {
        return adminService.getAdminById(id);
    }

    @Override
    public List<AdminDTO> findAll() {
        return adminConverter.toDTOList(adminService.list());
    }

    @Override
    public AdminDTO create(AdminUpsertRequestDTO requestDTO) {
        return adminService.createAdmin(requestDTO);
    }

    @Override
    public AdminDTO update(Long id, AdminUpsertRequestDTO requestDTO) {
        boolean success = adminService.updateAdmin(id, requestDTO);
        return success ? adminService.getAdminById(id) : null;
    }

    @Override
    public Boolean delete(Long id) {
        return adminService.deleteAdmin(id);
    }
}

