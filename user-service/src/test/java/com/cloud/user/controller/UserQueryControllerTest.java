package com.cloud.user.controller;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.vo.user.UserVO;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.user.controller.user.UserQueryController;
import com.cloud.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UserQueryController 单元测试
 *
 * @author Claude Code
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户查询控制器单元测试")
class UserQueryControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserQueryController userQueryController;

    private UserDTO testUserDTO;
    private UserVO testUserVO;

    @BeforeEach
    void setUp() {
        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setUsername("testuser");
        testUserDTO.setNickname("测试用户");
        testUserDTO.setPhone("13800138000");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setStatus(1);
        testUserDTO.setUserType("USER");

        testUserVO = new UserVO();
        testUserVO.setId(1L);
        testUserVO.setUsername("testuser");
        testUserVO.setNickname("测试用户");
    }

    @Test
    @DisplayName("根据用户名查询用户-成功")
    void testFindByUsername_Success() {
        // Given
        String username = "testuser";
        when(userService.findByUsername(username)).thenReturn(testUserDTO);

        // When
        Result<UserDTO> result = userQueryController.findByUsername(username);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals("查询成功", result.getMessage());
        assertEquals(testUserDTO, result.getData());
        verify(userService).findByUsername(username);
    }

    @Test
    @DisplayName("分页查询用户-成功")
    void testSearch_Success() {
        // Given
        Integer page = 1;
        Integer size = 20;
        String username = "test";

        PageResult<UserVO> pageResult = PageResult.of(1L, 20L, 1L, Arrays.asList(testUserVO));
        when(userService.pageQuery(any(UserPageDTO.class))).thenReturn(pageResult);

        // When
        Result<PageResult<UserVO>> result = userQueryController.search(page, size, username, null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getTotal());
        verify(userService).pageQuery(any(UserPageDTO.class));
    }

    @Test
    @DisplayName("根据GitHub ID查询用户-成功")
    void testFindByGitHubId_Success() {
        // Given
        Long githubId = 12345L;
        when(userService.findByGitHubId(githubId)).thenReturn(testUserDTO);

        // When
        Result<UserDTO> result = userQueryController.findByGitHubId(githubId);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals("查询成功", result.getMessage());
        assertEquals(testUserDTO, result.getData());
        verify(userService).findByGitHubId(githubId);
    }

    @Test
    @DisplayName("根据GitHub ID查询用户-未找到")
    void testFindByGitHubId_NotFound() {
        // Given
        Long githubId = 999L;
        when(userService.findByGitHubId(githubId)).thenReturn(null);

        // When
        Result<UserDTO> result = userQueryController.findByGitHubId(githubId);

        // Then
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertEquals(404, result.getCode());
        assertEquals("未找到对应的GitHub用户", result.getMessage());
        verify(userService).findByGitHubId(githubId);
    }

    @Test
    @DisplayName("根据GitHub用户名查询用户-成功")
    void testFindByGitHubUsername_Success() {
        // Given
        String githubUsername = "testuser";
        when(userService.findByGitHubUsername(githubUsername)).thenReturn(testUserDTO);

        // When
        Result<UserDTO> result = userQueryController.findByGitHubUsername(githubUsername);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals("查询成功", result.getMessage());
        assertEquals(testUserDTO, result.getData());
        verify(userService).findByGitHubUsername(githubUsername);
    }

    @Test
    @DisplayName("根据GitHub用户名查询用户-未找到")
    void testFindByGitHubUsername_NotFound() {
        // Given
        String githubUsername = "nonexistent";
        when(userService.findByGitHubUsername(githubUsername)).thenReturn(null);

        // When
        Result<UserDTO> result = userQueryController.findByGitHubUsername(githubUsername);

        // Then
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertEquals(404, result.getCode());
        assertEquals("未找到对应的GitHub用户", result.getMessage());
        verify(userService).findByGitHubUsername(githubUsername);
    }

    @Test
    @DisplayName("根据OAuth提供商查询用户-成功")
    void testFindByOAuthProvider_Success() {
        // Given
        String provider = "github";
        String providerId = "12345";
        when(userService.findByOAuthProvider(provider, providerId)).thenReturn(testUserDTO);

        // When
        Result<UserDTO> result = userQueryController.findByOAuthProvider(provider, providerId);

        // Then
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals("查询成功", result.getMessage());
        assertEquals(testUserDTO, result.getData());
        verify(userService).findByOAuthProvider(provider, providerId);
    }

    @Test
    @DisplayName("根据OAuth提供商查询用户-未找到")
    void testFindByOAuthProvider_NotFound() {
        // Given
        String provider = "github";
        String providerId = "999";
        when(userService.findByOAuthProvider(provider, providerId)).thenReturn(null);

        // When
        Result<UserDTO> result = userQueryController.findByOAuthProvider(provider, providerId);

        // Then
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertEquals(404, result.getCode());
        assertEquals("未找到对应的OAuth用户", result.getMessage());
        verify(userService).findByOAuthProvider(provider, providerId);
    }
}
