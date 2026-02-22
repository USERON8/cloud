package com.cloud.user.controller;

import com.cloud.api.user.AdminFeignClient;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.user.converter.AdminConverter;
import com.cloud.user.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal admin endpoints for service-to-service calls.
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminFeignController implements AdminFeignClient {

    private final AdminService adminService;
    private final AdminConverter adminConverter;

    @Override
    @GetMapping("/query/getById/{id}")
    public AdminDTO findById(@PathVariable("id") Long id) {
        return adminService.getAdminById(id);
    }

    @Override
    @GetMapping("/query/getAll")
    public List<AdminDTO> findAll() {
        return adminConverter.toDTOList(adminService.list());
    }

    @Override
    @PostMapping("/manage/create")
    public AdminDTO create(@RequestBody AdminDTO adminDTO) {
        return adminService.createAdmin(adminDTO);
    }

    @Override
    @PutMapping("/manage/update/{id}")
    public AdminDTO update(@PathVariable("id") Long id, @RequestBody AdminDTO adminDTO) {
        adminDTO.setId(id);
        boolean success = adminService.updateAdmin(adminDTO);
        return success ? adminService.getAdminById(id) : null;
    }

    @Override
    @DeleteMapping("/manage/delete/{id}")
    public Boolean delete(@PathVariable("id") Long id) {
        return adminService.deleteAdmin(id);
    }
}
