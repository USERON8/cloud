package com.cloud.user.security;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.config.PermissionManager;
import com.cloud.common.domain.dto.user.MerchantAuthRequestDTO;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.MerchantAuthDTO;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.vo.user.UserStatisticsVO;
import com.cloud.user.config.ResourceServerConfig;
import com.cloud.user.config.TokenBlacklistChecker;
import com.cloud.user.controller.AdminController;
import com.cloud.user.controller.AdminFeignController;
import com.cloud.user.controller.ThreadPoolMonitorController;
import com.cloud.user.controller.merchant.MerchantAuthController;
import com.cloud.user.controller.merchant.MerchantController;
import com.cloud.user.controller.user.UserAddressController;
import com.cloud.user.controller.user.UserFeignController;
import com.cloud.user.controller.user.UserManageController;
import com.cloud.user.controller.user.UserNotificationController;
import com.cloud.user.controller.user.UserProfileController;
import com.cloud.user.controller.user.UserStatisticsController;
import com.cloud.user.converter.AdminConverter;
import com.cloud.user.converter.MerchantAuthConverter;
import com.cloud.user.module.entity.Merchant;
import com.cloud.user.module.entity.MerchantAuth;
import com.cloud.user.module.entity.UserAddress;
import com.cloud.user.mapper.AdminMapper;
import com.cloud.user.mapper.MerchantAuthMapper;
import com.cloud.user.mapper.MerchantMapper;
import com.cloud.user.mapper.UserAddressMapper;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.service.AdminService;
import com.cloud.user.service.MerchantAuthService;
import com.cloud.user.service.MerchantService;
import com.cloud.user.service.MinioService;
import com.cloud.user.service.UserAddressService;
import com.cloud.user.service.UserAsyncService;
import com.cloud.user.service.UserNotificationService;
import com.cloud.user.service.UserService;
import com.cloud.user.service.UserStatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AdminController.class,
        UserManageController.class,
        UserProfileController.class,
        UserNotificationController.class,
        UserStatisticsController.class,
        ThreadPoolMonitorController.class,
        MerchantController.class,
        MerchantAuthController.class,
        UserAddressController.class,
        AdminFeignController.class,
        UserFeignController.class
}, properties = {
        "minio.endpoint=http://127.0.0.1:9000",
        "minio.access-key=test-access-key",
        "minio.secret-key=test-secret-key",
        "minio.public-endpoint=http://127.0.0.1:9000",
        "minio.bucket-name=test-bucket"
})
@Import({
        ResourceServerConfig.class,
        PermissionManager.class,
        UserServicePermissionMatrixTest.MethodSecurityTestConfig.class
})
class UserServicePermissionMatrixTest {

    @TestConfiguration
    @EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenBlacklistChecker tokenBlacklistChecker;

    @MockBean(name = "redisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean(name = "redissonClient")
    private RedissonClient redissonClient;

    @MockBean
    private AdminService adminService;

    @MockBean
    private UserService userService;

    @MockBean
    private MinioService minioService;

    @MockBean
    private UserNotificationService userNotificationService;

    @MockBean
    private UserStatisticsService userStatisticsService;

    @MockBean
    private UserAsyncService userAsyncService;

    @MockBean
    private MerchantService merchantService;

    @MockBean
    private MerchantAuthService merchantAuthService;

    @MockBean
    private MerchantAuthConverter merchantAuthConverter;

    @MockBean
    private AdminConverter adminConverter;

    @MockBean
    private UserAddressService userAddressService;

    @MockBean
    private ApplicationContext applicationContext;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private MerchantMapper merchantMapper;

    @MockBean
    private MerchantAuthMapper merchantAuthMapper;

    @MockBean
    private AdminMapper adminMapper;

    @MockBean
    private UserAddressMapper userAddressMapper;

    @Test
    void anonymousShouldBeRejectedForProtectedApi() throws Exception {
        mockMvc.perform(get("/api/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userShouldNotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin").with(userJwt("100")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminReadShouldAccessAdminList() throws Exception {
        when(adminService.getAdminsPage(anyInt(), anyInt())).thenReturn(new Page<>(1, 10, 0));

        mockMvc.perform(get("/api/admin").with(adminReadJwt("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void adminReadShouldNotCallWriteUserManageApi() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "user_for_update",
                "nickname", "nick_for_update"
        ));

        mockMvc.perform(put("/api/manage/users/200")
                        .with(adminReadJwt("1"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminWriteShouldCallWriteUserManageApi() throws Exception {
        when(userService.updateUser(any(UserDTO.class))).thenReturn(true);
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "user_for_update",
                "nickname", "nick_for_update"
        ));

        mockMvc.perform(put("/api/manage/users/200")
                        .with(adminWriteJwt("1"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void merchantWithoutReadScopeShouldNotListMerchants() throws Exception {
        mockMvc.perform(get("/api/merchant").with(merchantRoleOnlyJwt("200")))
                .andExpect(status().isForbidden());
    }

    @Test
    void merchantReadShouldOnlySeeSelfInMerchantList() throws Exception {
        MerchantDTO dto = new MerchantDTO();
        dto.setId(200L);
        dto.setMerchantName("merchant_self");
        when(merchantService.getMerchantById(200L)).thenReturn(dto);

        mockMvc.perform(get("/api/merchant").with(merchantReadJwt("200")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(merchantService, never()).getMerchantsPage(anyInt(), anyInt(), any());
    }

    @Test
    void merchantReadShouldNotAccessOtherMerchant() throws Exception {
        mockMvc.perform(get("/api/merchant/201").with(merchantReadJwt("200")))
                .andExpect(status().isForbidden());
    }

    @Test
    void merchantReadShouldNotUpdateMerchant() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "merchantName", "merchant_update"
        ));
        mockMvc.perform(put("/api/merchant/200")
                        .with(merchantReadJwt("200"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void merchantWriteShouldUpdateSelfMerchant() throws Exception {
        when(merchantService.updateMerchant(any(MerchantDTO.class))).thenReturn(true);
        String body = objectMapper.writeValueAsString(Map.of(
                "merchantName", "merchant_update"
        ));
        mockMvc.perform(put("/api/merchant/200")
                        .with(merchantWriteJwt("200"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void adminReadShouldNotCreateMerchant() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "new_merchant",
                "password", "Abcd1234",
                "merchantName", "new_merchant_name"
        ));
        mockMvc.perform(post("/api/merchant")
                        .with(adminReadJwt("1"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminWriteShouldCreateMerchant() throws Exception {
        MerchantDTO created = new MerchantDTO();
        created.setId(300L);
        created.setMerchantName("new_merchant_name");
        when(merchantService.createMerchant(any(MerchantDTO.class))).thenReturn(created);

        String body = objectMapper.writeValueAsString(Map.of(
                "username", "new_merchant",
                "password", "Abcd1234",
                "merchantName", "new_merchant_name"
        ));
        mockMvc.perform(post("/api/merchant")
                        .with(adminWriteJwt("1"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void normalUserShouldNotApplyMerchantAuth() throws Exception {
        String body = objectMapper.writeValueAsString(validMerchantAuthApplyBody());

        mockMvc.perform(post("/api/merchant/auth/apply/200")
                        .with(userJwt("100"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void merchantRoleOnlyShouldNotApplyMerchantAuth() throws Exception {
        String body = objectMapper.writeValueAsString(validMerchantAuthApplyBody());

        mockMvc.perform(post("/api/merchant/auth/apply/200")
                        .with(merchantRoleOnlyJwt("200"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void merchantReadShouldNotApplyMerchantAuth() throws Exception {
        String body = objectMapper.writeValueAsString(validMerchantAuthApplyBody());

        mockMvc.perform(post("/api/merchant/auth/apply/200")
                        .with(merchantReadJwt("200"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void merchantShouldApplyOwnMerchantAuth() throws Exception {
        when(merchantService.getById(200L)).thenReturn(new Merchant());
        when(merchantAuthService.getOne(any())).thenReturn(null);
        when(merchantAuthService.save(any(MerchantAuth.class))).thenReturn(true);
        when(merchantService.updateMerchantStatus(eq(200L), eq(0))).thenReturn(true);

        MerchantAuth entity = new MerchantAuth();
        when(merchantAuthConverter.toEntity(any(MerchantAuthRequestDTO.class))).thenReturn(entity);
        MerchantAuthDTO dto = new MerchantAuthDTO();
        dto.setMerchantId(200L);
        when(merchantAuthConverter.toDTO(any(MerchantAuth.class))).thenReturn(dto);

        String body = objectMapper.writeValueAsString(validMerchantAuthApplyBody());
        mockMvc.perform(post("/api/merchant/auth/apply/200")
                        .with(merchantWriteJwt("200"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void merchantReadShouldGetOwnMerchantAuth() throws Exception {
        MerchantAuth merchantAuth = new MerchantAuth();
        merchantAuth.setMerchantId(200L);
        when(merchantAuthService.getOne(any())).thenReturn(merchantAuth);
        MerchantAuthDTO dto = new MerchantAuthDTO();
        dto.setMerchantId(200L);
        when(merchantAuthConverter.toDTO(any(MerchantAuth.class))).thenReturn(dto);

        mockMvc.perform(get("/api/merchant/auth/get/200")
                        .with(merchantReadJwt("200")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void merchantRoleOnlyShouldNotGetMerchantAuth() throws Exception {
        mockMvc.perform(get("/api/merchant/auth/get/200")
                        .with(merchantRoleOnlyJwt("200")))
                .andExpect(status().isForbidden());
    }

    @Test
    void merchantReadShouldNotRevokeMerchantAuth() throws Exception {
        mockMvc.perform(delete("/api/merchant/auth/revoke/200")
                        .with(merchantReadJwt("200")))
                .andExpect(status().isForbidden());
    }

    @Test
    void userShouldNotReadOtherUserAddressList() throws Exception {
        mockMvc.perform(get("/api/user/address/list/101").with(userJwt("100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void userShouldReadOwnAddressList() throws Exception {
        when(userAddressService.list(Mockito.<Wrapper<UserAddress>>any())).thenReturn(List.of(new UserAddress()));

        mockMvc.perform(get("/api/user/address/list/100").with(userJwt("100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void userShouldReadOwnProfile() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(100L);
        userDTO.setUsername("u100");
        userDTO.setNickname("user_100");
        when(userService.getUserById(100L)).thenReturn(userDTO);

        mockMvc.perform(get("/api/user/profile/current").with(userJwt("100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void anonymousShouldNotUploadAvatar() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "fake-image".getBytes()
        );

        mockMvc.perform(multipart("/api/user/profile/current/avatar").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userShouldUploadOwnAvatar() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "fake-image".getBytes()
        );
        when(minioService.uploadAvatar(any())).thenReturn("http://127.0.0.1:9000/user-avatars/avatar/100/new.png");

        mockMvc.perform(multipart("/api/user/profile/current/avatar")
                        .file(file)
                        .with(userJwt("100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void adminWriteShouldSendSystemAnnouncement() throws Exception {
        when(userNotificationService.sendSystemAnnouncementAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(true));

        String body = objectMapper.writeValueAsString(Map.of(
                "title", "System Notice",
                "content", "Planned maintenance window"
        ));

        mockMvc.perform(post("/api/user/notification/system")
                        .with(adminWriteJwt("1"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void adminReadShouldNotSendSystemAnnouncement() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "System Notice",
                "content", "Planned maintenance window"
        ));

        mockMvc.perform(post("/api/user/notification/system")
                        .with(adminReadJwt("1"))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminReadShouldAccessStatistics() throws Exception {
        when(userStatisticsService.getUserStatisticsOverview()).thenReturn(new UserStatisticsVO());

        mockMvc.perform(get("/api/statistics/overview").with(adminReadJwt("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void adminWriteOnlyShouldNotAccessStatisticsReadApi() throws Exception {
        mockMvc.perform(get("/api/statistics/overview").with(adminWriteJwt("1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminReadShouldAccessThreadPoolApi() throws Exception {
        when(applicationContext.getBeansOfType(any())).thenReturn(Map.of());

        mockMvc.perform(get("/api/thread-pool/info").with(adminReadJwt("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void normalUserShouldNotAccessThreadPoolApi() throws Exception {
        mockMvc.perform(get("/api/thread-pool/info").with(userJwt("100")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousShouldNotAccessInternalUserApi() throws Exception {
        mockMvc.perform(get("/internal/user/id/100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void normalUserShouldNotAccessInternalUserApi() throws Exception {
        mockMvc.perform(get("/internal/user/id/100").with(userJwt("100")))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalTokenShouldAccessInternalUserApi() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(100L);
        userDTO.setUsername("u100");
        when(userService.getUserById(100L)).thenReturn(userDTO);

        mockMvc.perform(get("/internal/user/id/100").with(internalApiJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    void normalAdminScopeShouldNotAccessInternalAdminApi() throws Exception {
        mockMvc.perform(get("/admin/query/getById/1").with(adminReadJwt("1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalTokenShouldAccessInternalAdminApi() throws Exception {
        AdminDTO adminDTO = new AdminDTO();
        adminDTO.setId(1L);
        adminDTO.setUsername("admin1");
        when(adminService.getAdminById(eq(1L))).thenReturn(adminDTO);

        mockMvc.perform(get("/admin/query/getById/1").with(internalApiJwt()))
                .andExpect(status().isOk());

        verify(adminService).getAdminById(eq(1L));
    }

    private static Map<String, Object> validMerchantAuthApplyBody() {
        return Map.of(
                "businessLicenseNumber", "BL-123456",
                "businessLicenseUrl", "https://example.com/license.png",
                "idCardFrontUrl", "https://example.com/idf.png",
                "idCardBackUrl", "https://example.com/idb.png",
                "contactPhone", "13812345678",
                "contactAddress", "Shenzhen Nanshan"
        );
    }

    private static RequestPostProcessor userJwt(String userId) {
        return jwtToken(userId, "USER", "ROLE_USER", "SCOPE_user:read");
    }

    private static RequestPostProcessor adminReadJwt(String userId) {
        return jwtToken(userId, "ADMIN", "ROLE_ADMIN", "SCOPE_admin:read");
    }

    private static RequestPostProcessor adminWriteJwt(String userId) {
        return jwtToken(userId, "ADMIN", "ROLE_ADMIN", "SCOPE_admin:write");
    }

    private static RequestPostProcessor merchantRoleOnlyJwt(String userId) {
        return jwtToken(userId, "MERCHANT", "ROLE_MERCHANT");
    }

    private static RequestPostProcessor merchantReadJwt(String userId) {
        return jwtToken(userId, "MERCHANT", "ROLE_MERCHANT", "SCOPE_merchant:read");
    }

    private static RequestPostProcessor merchantWriteJwt(String userId) {
        return jwtToken(userId, "MERCHANT", "ROLE_MERCHANT", "SCOPE_merchant:write");
    }

    private static RequestPostProcessor internalApiJwt() {
        return jwtToken("0", "SYSTEM", "SCOPE_internal_api");
    }

    private static RequestPostProcessor jwtToken(String userId, String userType, String... authorities) {
        List<GrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt
                        .claim("user_id", userId)
                        .claim("user_type", userType)
                        .claim("username", "test_" + userId))
                .authorities(grantedAuthorities);
    }
}
