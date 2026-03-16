package com.cloud.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.user.converter.AdminConverter;
import com.cloud.user.exception.AdminException;
import com.cloud.user.mapper.AdminMapper;
import com.cloud.user.module.entity.Admin;
import com.cloud.user.service.AdminService;
import com.cloud.user.service.support.AuthPrincipalService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

  private static final String ADMIN_CACHE = "admin";
  private static final Integer STATUS_ENABLED = 1;
  private static final Integer STATUS_DISABLED = 0;

  private final AdminMapper adminMapper;
  private final AdminConverter adminConverter;
  private final AuthPrincipalService authPrincipalService;
  private final CacheManager cacheManager;

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
  public List<AdminDTO> listAdmins() {
    List<Admin> admins = list();
    if (admins == null || admins.isEmpty()) {
      return List.of();
    }
    return adminConverter.toDTOList(admins);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AdminDTO> getAdminsPage(Integer page, Integer size) {
    Page<Admin> pageParam = new Page<>(page, size);
    Page<Admin> adminPage = lambdaQuery().orderByDesc(Admin::getId).page(pageParam);

    Page<AdminDTO> dtoPage =
        new Page<>(adminPage.getCurrent(), adminPage.getSize(), adminPage.getTotal());
    dtoPage.setRecords(
        adminPage.getRecords().stream().map(adminConverter::toDTO).collect(Collectors.toList()));
    return dtoPage;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  @CachePut(cacheNames = ADMIN_CACHE, key = "#result.id")
  @DistributedLock(
      key = "'create:' + #requestDTO.username",
      prefix = "admin",
      waitTime = 10,
      leaseTime = 30,
      failMessage = "failed to acquire create admin lock")
  public AdminDTO createAdmin(AdminUpsertRequestDTO requestDTO)
      throws AdminException.AdminAlreadyExistsException {
    if (StrUtil.isBlank(requestDTO.getUsername())) {
      throw new IllegalArgumentException("username is required");
    }
    if (StrUtil.isBlank(requestDTO.getRealName())) {
      throw new IllegalArgumentException("realName is required");
    }
    if (StrUtil.isBlank(requestDTO.getPassword())) {
      throw new IllegalArgumentException("password is required");
    }

    long count = lambdaQuery().eq(Admin::getUsername, requestDTO.getUsername()).count();
    if (count > 0) {
      log.warn("Admin already exists, username={}", requestDTO.getUsername());
      throw new AdminException.AdminAlreadyExistsException(requestDTO.getUsername());
    }
    authPrincipalService.assertUsernameAvailable(requestDTO.getUsername(), null);

    Admin admin = toAdminEntity(requestDTO);
    if (admin.getStatus() == null) {
      admin.setStatus(STATUS_ENABLED);
    }

    if (!save(admin)) {
      throw new AdminException("failed to create admin");
    }
    authPrincipalService.createPrincipal(toAuthPrincipalDTO(admin, requestDTO.getPassword()));
    return adminConverter.toDTO(admin);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  @Caching(
      evict = {
        @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id"),
        @CacheEvict(cacheNames = ADMIN_CACHE, key = "'username:' + #requestDTO.username")
      })
  @DistributedLock(
      key = "'update:' + #id",
      prefix = "admin",
      waitTime = 10,
      leaseTime = 30,
      failMessage = "failed to acquire update admin lock")
  public boolean updateAdmin(Long id, AdminUpsertRequestDTO requestDTO)
      throws AdminException.AdminNotFoundException {
    if (id == null || requestDTO == null) {
      throw new IllegalArgumentException("admin id is required");
    }
    Admin existingAdmin = getById(id);
    if (existingAdmin == null) {
      log.warn("Admin not found, adminId={}", id);
      throw new AdminException.AdminNotFoundException(id);
    }

    if (StrUtil.isNotBlank(requestDTO.getUsername())
        && !requestDTO.getUsername().equals(existingAdmin.getUsername())) {
      long count =
          lambdaQuery()
              .eq(Admin::getUsername, requestDTO.getUsername())
              .ne(Admin::getId, id)
              .count();
      if (count > 0) {
        throw new AdminException.AdminAlreadyExistsException(requestDTO.getUsername());
      }
      authPrincipalService.assertUsernameAvailable(requestDTO.getUsername(), id);
    }

    Admin admin = toAdminEntity(requestDTO);
    admin.setId(id);
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

    boolean updated = updateById(admin);
    if (updated) {
      Admin current = resolveCurrentAdmin(requestDTO, existingAdmin);
      authPrincipalService.updatePrincipal(toAuthPrincipalDTO(current, requestDTO.getPassword()));
      evictUsernameCacheIfChanged(existingAdmin.getUsername(), requestDTO.getUsername());
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
      failMessage = "failed to acquire delete admin lock")
  public boolean deleteAdmin(Long id) throws AdminException.AdminNotFoundException {
    Admin admin = getById(id);
    if (admin == null) {
      log.warn("Admin not found, adminId={}", id);
      throw new AdminException.AdminNotFoundException(id);
    }
    boolean removed = removeById(id);
    if (removed) {
      authPrincipalService.deletePrincipal(id);
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
    boolean removed = removeByIds(ids);
    if (removed) {
      ids.forEach(
          id -> {
            authPrincipalService.deletePrincipal(id);
          });
    }
    return removed;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
  public boolean updateAdminStatus(Long id, Integer status)
      throws AdminException.AdminNotFoundException {
    Admin admin = getById(id);
    if (admin == null) {
      log.warn("Admin not found, adminId={}", id);
      throw new AdminException.AdminNotFoundException(id);
    }
    admin.setStatus(status);
    boolean updated = updateById(admin);
    if (updated) {
      AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
      authPrincipalDTO.setId(admin.getId());
      authPrincipalDTO.setStatus(admin.getStatus());
      authPrincipalService.updatePrincipal(authPrincipalDTO);
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
  public boolean resetPassword(Long id, String newPassword)
      throws AdminException.AdminNotFoundException {
    if (StrUtil.isBlank(newPassword)) {
      throw new IllegalArgumentException("new password is required");
    }

    Admin admin = getById(id);
    if (admin == null) {
      log.warn("Admin not found, adminId={}", id);
      throw new AdminException.AdminNotFoundException(id);
    }

    AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
    authPrincipalDTO.setId(admin.getId());
    authPrincipalDTO.setPassword(newPassword);
    authPrincipalService.updatePrincipal(authPrincipalDTO);
    return true;
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

    if (!authPrincipalService.changePassword(id, oldPassword, newPassword)) {
      log.warn("Old password mismatch, adminId={}", id);
      throw new AdminException.AdminPasswordException("old password mismatch");
    }
    return true;
  }

  @Override
  @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
  public void evictAdminCache(Long id) {}

  @Override
  @CacheEvict(cacheNames = ADMIN_CACHE, allEntries = true)
  public void evictAllAdminCache() {}

  private Admin toAdminEntity(AdminUpsertRequestDTO requestDTO) {
    Admin admin = new Admin();
    admin.setUsername(requestDTO.getUsername());
    admin.setRealName(requestDTO.getRealName());
    admin.setPhone(requestDTO.getPhone());
    admin.setRole(requestDTO.getRole());
    admin.setStatus(requestDTO.getStatus());
    return admin;
  }

  private Admin resolveCurrentAdmin(AdminUpsertRequestDTO requestDTO, Admin existingAdmin) {
    Admin merged = new Admin();
    merged.setId(existingAdmin.getId());
    merged.setUsername(
        StrUtil.blankToDefault(requestDTO.getUsername(), existingAdmin.getUsername()));
    merged.setRealName(
        StrUtil.blankToDefault(requestDTO.getRealName(), existingAdmin.getRealName()));
    merged.setPhone(
        requestDTO.getPhone() == null ? existingAdmin.getPhone() : requestDTO.getPhone());
    merged.setRole(StrUtil.blankToDefault(requestDTO.getRole(), existingAdmin.getRole()));
    merged.setStatus(
        requestDTO.getStatus() == null ? existingAdmin.getStatus() : requestDTO.getStatus());
    return merged;
  }

  private AuthPrincipalDTO toAuthPrincipalDTO(Admin admin, String password) {
    AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
    authPrincipalDTO.setId(admin.getId());
    authPrincipalDTO.setUsername(admin.getUsername());
    authPrincipalDTO.setPassword(password);
    authPrincipalDTO.setNickname(admin.getRealName());
    authPrincipalDTO.setPhone(admin.getPhone());
    authPrincipalDTO.setStatus(admin.getStatus());
    authPrincipalDTO.setRoles(resolveAdminRoles(admin.getRole()));
    return authPrincipalDTO;
  }

  private void evictUsernameCacheIfChanged(String oldUsername, String newUsername) {
    if (StrUtil.isBlank(oldUsername) || StrUtil.isBlank(newUsername)) {
      return;
    }
    if (StrUtil.equals(oldUsername, newUsername)) {
      return;
    }
    Cache cache = cacheManager.getCache(ADMIN_CACHE);
    if (cache != null) {
      cache.evict("username:" + oldUsername);
    }
  }

  private List<String> resolveAdminRoles(String role) {
    return List.of("ROLE_ADMIN");
  }
}
