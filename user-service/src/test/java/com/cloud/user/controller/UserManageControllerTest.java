package com.cloud.user.controller;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.UserType;
import com.cloud.common.result.Result;
import com.cloud.user.controller.user.UserManageController;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserManageController 单元测试
 *
 * @author Claude Code
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户管理控制器单元测试")
class UserManageControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserManageController userManageController;

    private UserDTO testUserDTO;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setUsername("testuser");
        testUserDTO.setNickname("测试用户");
        testUserDTO.setPhone("13800138000");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setStatus(1);
        testUserDTO.setUserType(UserType.USER);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setNickname("测试用户");
        testUser.setPhone("13800138000");
        testUser.setEmail("test@example.com");
        testUser.setStatus(1);
        testUser.setUserType("USER");

        when(authentication.getName()).thenReturn("admin");
    }

    @Test
    @DisplayName("更新用户信息-成功")
    void testUpdate_Success() {
        // Given
        Long userId = 1L;
        when(userService.updateById(any(User.class))).thenReturn(true);

        // When
        Result<Boolean> result = userManageController.update(userId, testUserDTO, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("用户更新成功", result.getMessage());
        assertTrue(result.getData());
        verify(userService).updateById(any(User.class));
    }

    @Test
    @DisplayName("删除用户-成功")
    void testDelete_Success() {
        // Given
        Long userId = 1L;
        when(userService.deleteUserById(userId)).thenReturn(true);

        // When
        Result<Boolean> result = userManageController.delete(userId, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("用户删除成功", result.getMessage());
        assertTrue(result.getData());
        verify(userService).deleteUserById(userId);
    }

    @Test
    @DisplayName("批量删除用户-成功")
    void testDeleteBatch_Success() {
        // Given
        Long[] ids = {1L, 2L, 3L};
        when(userService.deleteUsersByIds(any(List.class))).thenReturn(true);

        // When
        Result<Boolean> result = userManageController.deleteBatch(ids, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("批量删除"));
        assertTrue(result.getData());
        verify(userService).deleteUsersByIds(any(List.class));
    }

    @Test
    @DisplayName("批量更新用户信息-成功")
    void testUpdateBatch_Success() {
        // Given
        List<UserDTO> userDTOList = Arrays.asList(testUserDTO);
        when(userService.updateBatchById(any(List.class))).thenReturn(true);

        // When
        Result<Boolean> result = userManageController.updateBatch(userDTOList, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("批量更新"));
        assertTrue(result.getData());
        verify(userService).updateBatchById(any(List.class));
    }

    @Test
    @DisplayName("批量更新用户状态-成功")
    void testUpdateStatusBatch_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        Integer status = 0;
        when(userService.batchUpdateUserStatus(ids, status)).thenReturn(3);

        // When
        Result<Boolean> result = userManageController.updateStatusBatch(ids, status, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("批量更新用户状态成功"));
        assertTrue(result.getData());
        verify(userService).batchUpdateUserStatus(ids, status);
    }

    @Test
    @DisplayName("批量更新用户状态-部分成功")
    void testUpdateStatusBatch_PartialSuccess() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        Integer status = 0;
        when(userService.batchUpdateUserStatus(ids, status)).thenReturn(2); // 只更新了2个

        // When
        Result<Boolean> result = userManageController.updateStatusBatch(ids, status, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("2/3"));
        verify(userService).batchUpdateUserStatus(ids, status);
    }
}
