package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.user.converter.AdminConverter;
import com.cloud.user.exception.AdminException;
import com.cloud.user.mapper.AdminMapper;
import com.cloud.user.module.entity.Admin;
import com.cloud.user.service.AdminService;
import com.cloud.user.service.support.AuthPrincipalRemoteService;
import com.cloud.user.service.support.UserPrincipalSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private static final String ADMIN_CACHE = "admin";
    private static final Integer STATUS_ENABLED = 1;
    private static final Integer STATUS_DISABLED = 0;

    private final AdminMapper adminMapper;
    private final AdminConverter adminConverter;
    private final PasswordEncoder passwordEncoder;
    private final AuthPrincipalRemoteService authPrincipalRemoteService;
    private final UserPrincipalSyncService userPrincipalSyncService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ADMIN_CACHE, key = "#id", unless = "#result == null")
    public AdminDTO getAdminById(Long id) throws AdminException.AdminNotFoundException {
        Admin admin = getById(id);
        if (admin == null) {
            log.warn("Admin not found, adminId={}", id);
            throw new AdminException.AdminNotFoundException(id);
        }
        return adminConverter.toDTO(admin);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ADMIN_CACHE, key = "'username:' + #username", unless = "#result == null")
    public AdminDTO getAdminByUsername(String username) throws AdminException.AdminNotFoundException {
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("username is required");
        }

        Admin admin = lambdaQuery().eq(Admin::getUsername, username).one();
        if (admin == null) {
            log.warn("Admin not found, username={}", username);
            throw new AdminException.AdminNotFoundException(username);
        }
        return adminConverter.toDTO(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDTO> getAdminsByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }

        List<Admin> admins = listByIds(ids);
        return admins.stream().map(adminConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminDTO> getMerchantsPage(Integer page, Integer size, Integer status) {
        Page<Admin> pageParam = new Page<>(page, size);
        Page<Admin> adminPage = lambdaQuery()
                .eq(status != null, Admin::getStatus, status)
                .orderByDesc(Admin::getId)
                .page(pageParam);

        Page<AdminDTO> dtoPage = new Page<>(adminPage.getCurrent(), adminPage.getSize(), adminPage.getTotal());
        dtoPage.setRecords(adminPage.getRecords().stream().map(adminConverter::toDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminDTO> getAdminsPage(Integer page, Integer size) {
        Page<Admin> pageParam = new Page<>(page, size);
        Page<Admin> adminPage = lambdaQuery().orderByDesc(Admin::getId).page(pageParam);

        Page<AdminDTO> dtoPage = new Page<>(adminPage.getCurrent(), adminPage.getSize(), adminPage.getTotal());
        dtoPage.setRecords(adminPage.getRecords().stream().map(adminConverter::toDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CachePut(cacheNames = ADMIN_CACHE, key = "#result.id")
    @DistributedLock(
            key = "'create:' + #adminDTO.username",
            prefix = "admin",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "failed to acquire create admin lock"
    )
    public AdminDTO createAdmin(AdminDTO adminDTO) throws AdminException.AdminAlreadyExistsException {
        if (StrUtil.isBlank(adminDTO.getUsername())) {
            throw new IllegalArgumentException("username is required");
        }
        if (StrUtil.isBlank(adminDTO.getRealName())) {
            throw new IllegalArgumentException("realName is required");
        }
        if (StrUtil.isBlank(adminDTO.getPassword())) {
            throw new IllegalArgumentException("password is required");
        }

        long count = lambdaQuery().eq(Admin::getUsername, adminDTO.getUsername()).count();
        if (count > 0) {
            log.warn("Admin already exists, username={}", adminDTO.getUsername());
            throw new AdminException.AdminAlreadyExistsException(adminDTO.getUsername());
        }
        authPrincipalRemoteService.assertUsernameAvailable(adminDTO.getUsername(), null);

        Admin admin = adminConverter.toEntity(adminDTO);
        if (StrUtil.isNotBlank(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        if (admin.getStatus() == null) {
            admin.setStatus(STATUS_ENABLED);
        }

        if (!save(admin)) {
            throw new AdminException("failed to create admin");
        }
        userPrincipalSyncService.upsertUserPrincipal(
                admin.getId(),
                admin.getUsername(),
                admin.getPassword(),
                admin.getRealName(),
                null,
                admin.getPhone(),
                admin.getStatus()
        );
        authPrincipalRemoteService.createPrincipal(toAuthPrincipalDTO(admin));
        return adminConverter.toDTO(admin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = ADMIN_CACHE, key = "#adminDTO.id"),
            @CacheEvict(cacheNames = ADMIN_CACHE, key = "'username:' + #adminDTO.username")
    })
    @DistributedLock(
            key = "'update:' + #adminDTO.id",
            prefix = "admin",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "failed to acquire update admin lock"
    )
    public boolean updateAdmin(AdminDTO adminDTO) throws AdminException.AdminNotFoundException {
        if (adminDTO == null || adminDTO.getId() == null) {
            throw new IllegalArgumentException("admin id is required");
        }
        Admin existingAdmin = getById(adminDTO.getId());
        if (existingAdmin == null) {
            log.warn("Admin not found, adminId={}", adminDTO.getId());
            throw new AdminException.AdminNotFoundException(adminDTO.getId());
        }

        if (StrUtil.isNotBlank(adminDTO.getUsername())
                && !adminDTO.getUsername().equals(existingAdmin.getUsername())) {
            long count = lambdaQuery()
                    .eq(Admin::getUsername, adminDTO.getUsername())
                    .ne(Admin::getId, adminDTO.getId())
                    .count();
            if (count > 0) {
                throw new AdminException.AdminAlreadyExistsException(adminDTO.getUsername());
            }
            authPrincipalRemoteService.assertUsernameAvailable(adminDTO.getUsername(), adminDTO.getId());
        }

        Admin admin = adminConverter.toEntity(adminDTO);
        admin.setId(adminDTO.getId());
        if (StrUtil.isBlank(admin.getUsername())) {
            admin.setUsername(existingAdmin.getUsername());
        }
        if (StrUtil.isBlank(admin.getRealName())) {
            admin.setRealName(existingAdmin.getRealName());
        }
        if (StrUtil.isBlank(admin.getRole())) {
            admin.setRole(existingAdmin.getRole());
        }
        if (admin.getStatus() == null) {
            admin.setStatus(existingAdmin.getStatus());
        }
        if (StrUtil.isBlank(admin.getPhone())) {
            admin.setPhone(existingAdmin.getPhone());
        }

        if (StrUtil.isNotBlank(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        } else {
            admin.setPassword(null);
        }

        boolean updated = updateById(admin);
        if (updated) {
            Admin current = resolveCurrentAdmin(adminDTO, existingAdmin);
            userPrincipalSyncService.upsertUserPrincipal(
                    current.getId(),
                    current.getUsername(),
                    current.getPassword(),
                    current.getRealName(),
                    null,
                    current.getPhone(),
                    current.getStatus()
            );
            authPrincipalRemoteService.updatePrincipal(toAuthPrincipalDTO(current));
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    @DistributedLock(
            key = "'delete:' + #id",
            prefix = "admin",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "failed to acquire delete admin lock"
    )
    public boolean deleteAdmin(Long id) throws AdminException.AdminNotFoundException {
        Admin admin = getById(id);
        if (admin == null) {
            log.warn("Admin not found, adminId={}", id);
            throw new AdminException.AdminNotFoundException(id);
        }
        boolean removed = removeById(id);
        if (removed) {
            userPrincipalSyncService.deleteUserPrincipal(id);
            authPrincipalRemoteService.deletePrincipal(id);
        }
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, allEntries = true)
    public boolean batchDeleteAdmins(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }
        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public boolean updateAdminStatus(Long id, Integer status) throws AdminException.AdminNotFoundException {
        Admin admin = getById(id);
        if (admin == null) {
            log.warn("Admin not found, adminId={}", id);
            throw new AdminException.AdminNotFoundException(id);
        }
        admin.setStatus(status);
        boolean updated = updateById(admin);
        if (updated) {
            userPrincipalSyncService.upsertUserPrincipal(
                    admin.getId(),
                    admin.getUsername(),
                    admin.getPassword(),
                    admin.getRealName(),
                    null,
                    admin.getPhone(),
                    admin.getStatus()
            );
            AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
            authPrincipalDTO.setId(admin.getId());
            authPrincipalDTO.setStatus(admin.getStatus());
            authPrincipalRemoteService.updatePrincipal(authPrincipalDTO);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public boolean enableAdmin(Long id) throws AdminException.AdminNotFoundException {
        return updateAdminStatus(id, STATUS_ENABLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public boolean disableAdmin(Long id) throws AdminException.AdminNotFoundException {
        return updateAdminStatus(id, STATUS_DISABLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public boolean resetPassword(Long id, String newPassword) throws AdminException.AdminNotFoundException {
        if (StrUtil.isBlank(newPassword)) {
            throw new IllegalArgumentException("new password is required");
        }

        Admin admin = getById(id);
        if (admin == null) {
            log.warn("Admin not found, adminId={}", id);
            throw new AdminException.AdminNotFoundException(id);
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        boolean updated = updateById(admin);
        if (updated) {
            userPrincipalSyncService.upsertUserPrincipal(
                    admin.getId(),
                    admin.getUsername(),
                    admin.getPassword(),
                    admin.getRealName(),
                    null,
                    admin.getPhone(),
                    admin.getStatus()
            );
            AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
            authPrincipalDTO.setId(admin.getId());
            authPrincipalDTO.setPassword(newPassword);
            authPrincipalRemoteService.updatePrincipal(authPrincipalDTO);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public boolean changePassword(Long id, String oldPassword, String newPassword)
            throws AdminException.AdminNotFoundException, AdminException.AdminPasswordException {
        if (StrUtil.isBlank(oldPassword) || StrUtil.isBlank(newPassword)) {
            throw new IllegalArgumentException("old password and new password are required");
        }

        Admin admin = getById(id);
        if (admin == null) {
            log.warn("Admin not found, adminId={}", id);
            throw new AdminException.AdminNotFoundException(id);
        }

        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            log.warn("Old password mismatch, adminId={}", id);
            throw new AdminException.AdminPasswordException("old password mismatch");
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        boolean updated = updateById(admin);
        if (updated) {
            userPrincipalSyncService.upsertUserPrincipal(
                    admin.getId(),
                    admin.getUsername(),
                    admin.getPassword(),
                    admin.getRealName(),
                    null,
                    admin.getPhone(),
                    admin.getStatus()
            );
            AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
            authPrincipalDTO.setId(admin.getId());
            authPrincipalDTO.setPassword(newPassword);
            authPrincipalRemoteService.updatePrincipal(authPrincipalDTO);
        }
        return updated;
    }

    @Override
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public void evictAdminCache(Long id) {
    }

    @Override
    @CacheEvict(cacheNames = ADMIN_CACHE, allEntries = true)
    public void evictAllAdminCache() {
    }

    private Admin resolveCurrentAdmin(AdminDTO adminDTO, Admin existingAdmin) {
        Admin merged = new Admin();
        merged.setId(existingAdmin.getId());
        merged.setUsername(StrUtil.blankToDefault(adminDTO.getUsername(), existingAdmin.getUsername()));
        merged.setPassword(StrUtil.isBlank(adminDTO.getPassword()) ? existingAdmin.getPassword() : passwordEncoder.encode(adminDTO.getPassword()));
        merged.setRealName(StrUtil.blankToDefault(adminDTO.getRealName(), existingAdmin.getRealName()));
        merged.setPhone(adminDTO.getPhone() == null ? existingAdmin.getPhone() : adminDTO.getPhone());
        merged.setRole(StrUtil.blankToDefault(adminDTO.getRole(), existingAdmin.getRole()));
        merged.setStatus(adminDTO.getStatus() == null ? existingAdmin.getStatus() : adminDTO.getStatus());
        return merged;
    }

    private AuthPrincipalDTO toAuthPrincipalDTO(Admin admin) {
        AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
        authPrincipalDTO.setId(admin.getId());
        authPrincipalDTO.setUsername(admin.getUsername());
        authPrincipalDTO.setPassword(admin.getPassword());
        authPrincipalDTO.setNickname(admin.getRealName());
        authPrincipalDTO.setPhone(admin.getPhone());
        authPrincipalDTO.setStatus(admin.getStatus());
        authPrincipalDTO.setRoles(resolveAdminRoles(admin.getRole()));
        return authPrincipalDTO;
    }

    private List<String> resolveAdminRoles(String role) {
        String normalized = StrUtil.blankToDefault(role, "ADMIN").trim().toUpperCase();
        return switch (normalized) {
            case "SUPER_ADMIN", "ROLE_SUPER_ADMIN" -> List.of("ADMIN", "SUPER_ADMIN");
            case "OPS_ADMIN", "ROLE_OPS_ADMIN" -> List.of("ADMIN", "OPS_ADMIN");
            default -> List.of("ADMIN");
        };
    }
}


