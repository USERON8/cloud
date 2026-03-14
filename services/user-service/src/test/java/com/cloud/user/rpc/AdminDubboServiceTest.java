package com.cloud.user.rpc;

import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.user.converter.AdminConverter;
import com.cloud.user.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDubboServiceTest {

    @Mock
    private AdminService adminService;

    @Mock
    private AdminConverter adminConverter;

    private AdminDubboService adminDubboService;

    @BeforeEach
    void setUp() {
        adminDubboService = new AdminDubboService(adminService, adminConverter);
    }

    @Test
    void findAll_usesConverter() {
        AdminDTO dto = new AdminDTO();
        when(adminService.list()).thenReturn(List.of());
        when(adminConverter.toDTOList(List.of())).thenReturn(List.of(dto));

        List<AdminDTO> result = adminDubboService.findAll();

        assertThat(result).containsExactly(dto);
        verify(adminConverter).toDTOList(List.of());
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
