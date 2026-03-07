package com.cloud.user.controller;

import com.cloud.api.user.AdminDubboApi;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.user.converter.AdminConverter;
import com.cloud.user.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;




@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDubboController implements AdminDubboApi {

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
    public Long create(@RequestBody AdminUpsertRequestDTO requestDTO) {
        AdminDTO adminDTO = adminService.createAdmin(requestDTO);
        return adminDTO == null ? null : adminDTO.getId();
    }

    @Override
    @PutMapping("/manage/update/{id}")
    public Boolean update(@PathVariable("id") Long id, @RequestBody AdminUpsertRequestDTO requestDTO) {
        return adminService.updateAdmin(id, requestDTO);
    }

    @Override
    @DeleteMapping("/manage/delete/{id}")
    public Boolean delete(@PathVariable("id") Long id) {
        return adminService.deleteAdmin(id);
    }
}

