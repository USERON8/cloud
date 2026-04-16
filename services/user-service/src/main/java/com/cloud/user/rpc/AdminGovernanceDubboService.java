package com.cloud.user.rpc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.api.user.AdminGovernanceDubboApi;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.common.domain.vo.user.AdminPageVO;
import com.cloud.user.service.AdminService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = AdminGovernanceDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class AdminGovernanceDubboService implements AdminGovernanceDubboApi {

  private final AdminService adminService;

  @Override
  public AdminPageVO getAdminsPage(Integer page, Integer size) {
    Page<AdminDTO> result = adminService.getAdminsPage(page, size);
    long pages = result.getPages();
    long current = result.getCurrent();
    return AdminPageVO.builder()
        .current(current)
        .size(result.getSize())
        .total(result.getTotal())
        .pages(pages)
        .records(result.getRecords())
        .hasPrevious(current > 1)
        .hasNext(current < pages)
        .build();
  }

  @Override
  public AdminDTO getAdminById(Long id) {
    return adminService.getAdminById(id);
  }

  @Override
  public AdminDTO createAdmin(AdminUpsertRequestDTO requestDTO) {
    return adminService.createAdmin(requestDTO);
  }

  @Override
  public Boolean updateAdmin(Long id, AdminUpsertRequestDTO requestDTO) {
    return adminService.updateAdmin(id, requestDTO);
  }

  @Override
  public Boolean deleteAdmin(Long id) {
    return adminService.deleteAdmin(id);
  }

  @Override
  public Boolean updateAdminStatus(Long id, Integer status) {
    return adminService.updateAdminStatus(id, status);
  }

  @Override
  public String resetPassword(Long id) {
    String temporaryPassword =
        "Tmp#" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    adminService.resetPassword(id, temporaryPassword);
    return temporaryPassword;
  }
}
