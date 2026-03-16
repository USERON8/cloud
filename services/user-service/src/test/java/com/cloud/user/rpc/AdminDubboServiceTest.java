package com.cloud.user.rpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.user.service.AdminService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDubboServiceTest {

  @Mock private AdminService adminService;

  private AdminDubboService adminDubboService;

  @BeforeEach
  void setUp() {
    adminDubboService = new AdminDubboService(adminService);
  }

  @Test
  void findAll_returnsAdmins() {
    AdminDTO dto = new AdminDTO();
    when(adminService.listAdmins()).thenReturn(List.of(dto));

    List<AdminDTO> result = adminDubboService.findAll();

    assertThat(result).containsExactly(dto);
    verify(adminService).listAdmins();
  }

  @Test
  void create_returnsId() {
    AdminUpsertRequestDTO request = new AdminUpsertRequestDTO();
    AdminDTO dto = new AdminDTO();
    dto.setId(7L);
    when(adminService.createAdmin(request)).thenReturn(dto);

    Long id = adminDubboService.create(request);

    assertThat(id).isEqualTo(7L);
  }
}
