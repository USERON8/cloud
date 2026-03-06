package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.user.converter.AdminConverter;
import com.cloud.user.exception.AdminException;
import com.cloud.user.mapper.AdminMapper;
import com.cloud.user.module.entity.Admin;
import com.cloud.user.service.AdminService;
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
import org.springframework.util.StringUtils;

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
        if (!StringUtils.hasText(username)) {
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
        if (!StringUtils.hasText(adminDTO.getUsername())) {
            throw new IllegalArgumentException("username is required");
        }
        if (!StringUtils.hasText(adminDTO.getRealName())) {
            throw new IllegalArgumentException("realName is required");
        }
        if (!StringUtils.hasText(adminDTO.getPassword())) {
            throw new IllegalArgumentException("password is required");
        }

        long count = lambdaQuery().eq(Admin::getUsername, adminDTO.getUsername()).count();
        if (count > 0) {
            log.warn("Admin already exists, username={}", adminDTO.getUsername());
            throw new AdminException.AdminAlreadyExistsException(adminDTO.getUsername());
        }

        Admin admin = adminConverter.toEntity(adminDTO);
        if (StringUtils.hasText(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        if (admin.getStatus() == null) {
            admin.setStatus(STATUS_ENABLED);
        }

        if (!save(admin)) {
            throw new AdminException("failed to create admin");
        }
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

        if (StringUtils.hasText(adminDTO.getUsername())
                && !adminDTO.getUsername().equals(existingAdmin.getUsername())) {
            long count = lambdaQuery()
                    .eq(Admin::getUsername, adminDTO.getUsername())
                    .ne(Admin::getId, adminDTO.getId())
                    .count();
            if (count > 0) {
                throw new AdminException.AdminAlreadyExistsException(adminDTO.getUsername());
            }
        }

        Admin admin = adminConverter.toEntity(adminDTO);
        admin.setId(adminDTO.getId());
        if (!StringUtils.hasText(admin.getUsername())) {
            admin.setUsername(existingAdmin.getUsername());
        }
        if (!StringUtils.hasText(admin.getRealName())) {
            admin.setRealName(existingAdmin.getRealName());
        }
        if (!StringUtils.hasText(admin.getRole())) {
            admin.setRole(existingAdmin.getRole());
        }
        if (admin.getStatus() == null) {
            admin.setStatus(existingAdmin.getStatus());
        }
        if (!StringUtils.hasText(admin.getPhone())) {
            admin.setPhone(existingAdmin.getPhone());
        }

        if (StringUtils.hasText(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        } else {
            admin.setPassword(null);
        }

        return updateById(admin);
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
        return removeById(id);
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
        return updateById(admin);
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
        if (!StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("new password is required");
        }

        Admin admin = getById(id);
        if (admin == null) {
            log.warn("Admin not found, adminId={}", id);
            throw new AdminException.AdminNotFoundException(id);
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        return updateById(admin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public boolean changePassword(Long id, String oldPassword, String newPassword)
            throws AdminException.AdminNotFoundException, AdminException.AdminPasswordException {
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
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
        return updateById(admin);
    }

    @Override
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public void evictAdminCache(Long id) {
    }

    @Override
    @CacheEvict(cacheNames = ADMIN_CACHE, allEntries = true)
    public void evictAllAdminCache() {
    }
}
