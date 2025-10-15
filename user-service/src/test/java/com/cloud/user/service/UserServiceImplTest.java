package com.cloud.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.auth.RegisterRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.vo.user.UserVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.user.converter.MerchantConverter;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 *
 * @author Claude Code
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务单元测试")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserConverter userConverter;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MerchantService merchantService;

    @Mock
    private MerchantConverter merchantConverter;

    private UserServiceImpl userService;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        // 手动创建 UserServiceImpl 实例
        userService = new UserServiceImpl(userConverter, passwordEncoder, merchantService, merchantConverter);
        // 使用反射注入 baseMapper (继承自 ServiceImpl 的字段)
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);

        // 准备测试数据
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setNickname("测试用户");
        testUser.setPhone("13800138000");
        testUser.setEmail("test@example.com");
        testUser.setStatus(1);
        testUser.setUserType("USER");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setUsername("testuser");
        testUserDTO.setNickname("测试用户");
        testUserDTO.setPhone("13800138000");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setStatus(1);
    }

    @Test
    @DisplayName("根据用户名查找用户-成功")
    void testFindByUsername_Success() {
        // Given
        String username = "testuser";
        when(userMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(testUser);
        when(userConverter.toDTO(testUser)).thenReturn(testUserDTO);

        // When
        UserDTO result = userService.findByUsername(username);

        // Then
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        verify(userConverter).toDTO(testUser);
    }

    @Test
    @DisplayName("根据用户名查找用户-用户名为空")
    void testFindByUsername_EmptyUsername() {
        // Given
        String username = "";

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.findByUsername(username));
        assertEquals("用户名不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("根据用户名查找用户-用户不存在")
    void testFindByUsername_UserNotFound() {
        // Given
        String username = "nonexistent";
        when(userMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        // When
        UserDTO result = userService.findByUsername(username);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("根据ID获取用户-成功")
    void testGetUserById_Success() {
        // Given
        Long userId = 1L;
        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userConverter.toDTO(testUser)).thenReturn(testUserDTO);

        // When
        UserDTO result = userService.getUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
        verify(userMapper).selectById(userId);
    }

    @Test
    @DisplayName("根据ID获取用户-用户不存在")
    void testGetUserById_UserNotFound() {
        // Given
        Long userId = 999L;
        when(userMapper.selectById(userId)).thenReturn(null);

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.getUserById(userId));
        assertTrue(exception.getMessage().contains("用户"));
    }

    @Test
    @DisplayName("根据ID获取用户-ID为空")
    void testGetUserById_NullId() {
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.getUserById(null));
        assertEquals("用户ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("用户注册-成功")
    void testRegisterUser_Success() {
        // Given
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setUserType("USER");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null); // 用户不存在
        when(userConverter.toEntity(registerRequest)).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(userConverter.toDTO(any(User.class))).thenReturn(testUserDTO);

        // When
        UserDTO result = userService.registerUser(registerRequest);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userMapper).insert(any(User.class));
        verify(passwordEncoder).encode(anyString());
    }

    @Test
    @DisplayName("用户注册-用户名已存在")
    void testRegisterUser_UsernameExists() {
        // Given
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername("existinguser");
        registerRequest.setPassword("password123");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(testUser); // 用户已存在

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.registerUser(registerRequest));
        assertTrue(exception.getMessage().contains("用户名已存在"));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("删除用户-成功")
    void testDeleteUserById_Success() {
        // Given
        Long userId = 1L;
        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userMapper.deleteById(userId)).thenReturn(1);

        // When
        boolean result = userService.deleteUserById(userId);

        // Then
        assertTrue(result);
        verify(userMapper).selectById(userId);
        verify(userMapper).deleteById(userId);
    }

    @Test
    @DisplayName("删除用户-用户不存在")
    void testDeleteUserById_UserNotFound() {
        // Given
        Long userId = 999L;
        when(userMapper.selectById(userId)).thenReturn(null);

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.deleteUserById(userId));
        assertTrue(exception.getMessage().contains("用户"));
        verify(userMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除用户-ID为空")
    void testDeleteUserById_NullId() {
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.deleteUserById(null));
        assertEquals("用户ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("批量更新用户状态-成功")
    void testBatchUpdateUserStatus_Success() {
        // Given
        Collection<Long> userIds = Arrays.asList(1L, 2L, 3L);
        Integer status = 0;

        when(userMapper.update(any(User.class), any(LambdaQueryWrapper.class))).thenReturn(3);

        // When
        Integer result = userService.batchUpdateUserStatus(userIds, status);

        // Then
        assertEquals(3, result);
        verify(userMapper).update(any(User.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("批量更新用户状态-用户ID集合为空")
    void testBatchUpdateUserStatus_EmptyIds() {
        // Given
        Collection<Long> userIds = Arrays.asList();
        Integer status = 0;

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.batchUpdateUserStatus(userIds, status));
        assertEquals("用户ID集合不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("批量更新用户状态-状态值为空")
    void testBatchUpdateUserStatus_NullStatus() {
        // Given
        Collection<Long> userIds = Arrays.asList(1L, 2L);
        Integer status = null;

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.batchUpdateUserStatus(userIds, status));
        assertEquals("状态值不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("分页查询用户-成功")
    void testPageQuery_Success() {
        // Given
        UserPageDTO pageDTO = new UserPageDTO();
        pageDTO.setCurrent(1L);
        pageDTO.setSize(10L);
        pageDTO.setUsername("test");

        Page<User> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testUser));
        page.setTotal(1);

        UserVO userVO = new UserVO();
        userVO.setId(1L);
        userVO.setUsername("testuser");

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(userConverter.toVOList(anyList())).thenReturn(Arrays.asList(userVO));

        // When
        PageResult<UserVO> result = userService.pageQuery(pageDTO);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        verify(userMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("修改密码-成功")
    void testChangePassword_Success() {
        // Given
        Long userId = 1L;
        String oldPassword = "oldPassword123";
        String newPassword = "newPassword456";

        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(passwordEncoder.matches(oldPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("$2a$10$newEncodedPassword");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        // When
        Boolean result = userService.changePassword(userId, oldPassword, newPassword);

        // Then
        assertTrue(result);
        verify(userMapper).selectById(userId);
        verify(passwordEncoder).matches(oldPassword, testUser.getPassword());
        verify(passwordEncoder).encode(newPassword);
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    @DisplayName("修改密码-旧密码错误")
    void testChangePassword_WrongOldPassword() {
        // Given
        Long userId = 1L;
        String oldPassword = "wrongPassword";
        String newPassword = "newPassword456";

        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(passwordEncoder.matches(oldPassword, testUser.getPassword())).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.changePassword(userId, oldPassword, newPassword));
        assertEquals("旧密码错误", exception.getMessage());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("修改密码-用户不存在")
    void testChangePassword_UserNotFound() {
        // Given
        Long userId = 999L;
        String oldPassword = "oldPassword123";
        String newPassword = "newPassword456";

        when(userMapper.selectById(userId)).thenReturn(null);

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.changePassword(userId, oldPassword, newPassword));
        assertTrue(exception.getMessage().contains("用户"));
    }

    @Test
    @DisplayName("批量查询用户-成功")
    void testGetUsersByIds_Success() {
        // Given
        Collection<Long> userIds = Arrays.asList(1L, 2L);
        List<User> users = Arrays.asList(testUser);

        when(userMapper.selectBatchIds(userIds)).thenReturn(users);
        when(userConverter.toDTOList(users)).thenReturn(Arrays.asList(testUserDTO));

        // When
        List<UserDTO> result = userService.getUsersByIds(userIds);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userMapper).selectBatchIds(userIds);
    }

    @Test
    @DisplayName("批量查询用户-ID集合为空")
    void testGetUsersByIds_EmptyIds() {
        // When
        List<UserDTO> result = userService.getUsersByIds(Arrays.asList());

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper, never()).selectBatchIds(anyCollection());
    }

    @Test
    @DisplayName("获取用户密码-成功")
    void testGetUserPassword_Success() {
        // Given
        String username = "testuser";
        when(userMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(testUser);

        // When
        String result = userService.getUserPassword(username);

        // Then
        assertNotNull(result);
        assertEquals("$2a$10$encodedPassword", result);
    }

    @Test
    @DisplayName("获取用户密码-用户不存在")
    void testGetUserPassword_UserNotFound() {
        // Given
        String username = "nonexistent";
        when(userMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        // When
        String result = userService.getUserPassword(username);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("获取用户密码-用户已禁用")
    void testGetUserPassword_UserDisabled() {
        // Given
        String username = "testuser";
        testUser.setStatus(0); // 禁用状态
        when(userMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(testUser);

        // When
        String result = userService.getUserPassword(username);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("更新用户状态-成功")
    void testUpdateUserStatus_Success() {
        // Given
        Long userId = 1L;
        Integer status = 0;

        when(userMapper.updateById(any(User.class))).thenReturn(1);

        // When
        Boolean result = userService.updateUserStatus(userId, status);

        // Then
        assertTrue(result);
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    @DisplayName("重置密码-成功")
    void testResetPassword_Success() {
        // Given
        Long userId = 1L;
        String defaultPassword = "123456";

        when(passwordEncoder.encode(defaultPassword)).thenReturn("$2a$10$encodedDefaultPassword");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        // When
        String result = userService.resetPassword(userId);

        // Then
        assertEquals(defaultPassword, result);
        verify(passwordEncoder).encode(defaultPassword);
        verify(userMapper).updateById(any(User.class));
    }
}
