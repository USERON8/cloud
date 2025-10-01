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

/**
 * 管理员服务实现类
 * 提供管理员相关的业务操作实现，包含分布式锁、缓存、事务等处理
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImplNew extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    // 缓存名称
    private static final String ADMIN_CACHE = "admin";
    private static final String ADMIN_LIST_CACHE = "admin:list";

    // 管理员状态
    private static final Integer STATUS_ENABLED = 1;
    private static final Integer STATUS_DISABLED = 0;

    private final AdminMapper adminMapper;
    private final AdminConverter adminConverter;
    private final PasswordEncoder passwordEncoder;

    // ================= 查询操作 =================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ADMIN_CACHE, key = "#id", unless = "#result == null")
    public AdminDTO getAdminById(Long id) throws AdminException.AdminNotFoundException {
        log.info("查询管理员信息, adminId: {}", id);

        Admin admin = getById(id);
        if (admin == null) {
            log.warn("管理员不存在, adminId: {}", id);
            throw new AdminException.AdminNotFoundException(id);
        }

        return adminConverter.toDTO(admin);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ADMIN_CACHE, key = "'username:' + #username", unless = "#result == null")
    public AdminDTO getAdminByUsername(String username) throws AdminException.AdminNotFoundException {
        log.info("根据用户名查询管理员, username: {}", username);

        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        Admin admin = lambdaQuery()
                .eq(Admin::getUsername, username)
                .one();

        if (admin == null) {
            log.warn("管理员不存在, username: {}", username);
            throw new AdminException.AdminNotFoundException(username);
        }

        return adminConverter.toDTO(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDTO> getAdminsByIds(List<Long> ids) {
        log.info("批量查询管理员, ids: {}", ids);

        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }

        List<Admin> admins = listByIds(ids);
        return admins.stream()
                .map(adminConverter::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminDTO> getMerchantsPage(Integer page, Integer size, Integer status) {
        log.info("分页查询管理员, page: {}, size: {}, status: {}", page, size, status);

        Page<Admin> pageParam = new Page<>(page, size);
        Page<Admin> adminPage = lambdaQuery()
                .eq(status != null, Admin::getStatus, status)
                .orderByDesc(Admin::getCreateTime)
                .page(pageParam);

        Page<AdminDTO> dtoPage = new Page<>(adminPage.getCurrent(), adminPage.getSize(), adminPage.getTotal());
        List<AdminDTO> dtoList = adminPage.getRecords().stream()
                .map(adminConverter::toDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    // ================= 创建和更新操作 =================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CachePut(cacheNames = ADMIN_CACHE, key = "#result.id")
    @DistributedLock(
            key = "'create:' + #adminDTO.username",
            prefix = "admin",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "创建管理员失败，请稍后重试"
    )
    public AdminDTO createAdmin(AdminDTO adminDTO) throws AdminException.AdminAlreadyExistsException {
        log.info("创建管理员, username: {}", adminDTO.getUsername());

        // 检查用户名是否已存在
        long count = lambdaQuery()
                .eq(Admin::getUsername, adminDTO.getUsername())
                .count();

        if (count > 0) {
            log.warn("管理员已存在, username: {}", adminDTO.getUsername());
            throw new AdminException.AdminAlreadyExistsException(adminDTO.getUsername());
        }

        // 转换并保存
        Admin admin = adminConverter.toEntity(adminDTO);

        // 加密密码
        if (StringUtils.hasText(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }

        // 设置默认状态
        if (admin.getStatus() == null) {
            admin.setStatus(STATUS_ENABLED);
        }

        if (!save(admin)) {
            throw new AdminException("创建管理员失败");
        }

        log.info("创建管理员成功, adminId: {}", admin.getId());
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
            failMessage = "更新管理员失败，请稍后重试"
    )
    public boolean updateAdmin(AdminDTO adminDTO) throws AdminException.AdminNotFoundException {
        log.info("更新管理员信息, adminId: {}", adminDTO.getId());

        // 检查管理员是否存在
        Admin existingAdmin = getById(adminDTO.getId());
        if (existingAdmin == null) {
            log.warn("管理员不存在, adminId: {}", adminDTO.getId());
            throw new AdminException.AdminNotFoundException(adminDTO.getId());
        }

        // 如果更新用户名，检查新用户名是否被占用
        if (StringUtils.hasText(adminDTO.getUsername()) &&
                !adminDTO.getUsername().equals(existingAdmin.getUsername())) {

            long count = lambdaQuery()
                    .eq(Admin::getUsername, adminDTO.getUsername())
                    .ne(Admin::getId, adminDTO.getId())
                    .count();

            if (count > 0) {
                throw new AdminException.AdminAlreadyExistsException(adminDTO.getUsername());
            }
        }

        // 转换并更新
        Admin admin = adminConverter.toEntity(adminDTO);
        admin.setId(adminDTO.getId());

        // 如果包含密码，则加密
        if (StringUtils.hasText(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        } else {
            admin.setPassword(null); // 不更新密码
        }

        boolean result = updateById(admin);
        log.info("更新管理员成功, adminId: {}", adminDTO.getId());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    @DistributedLock(
            key = "'delete:' + #id",
            prefix = "admin",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "删除管理员失败，请稍后重试"
    )
    public boolean deleteAdmin(Long id) throws AdminException.AdminNotFoundException {
        log.info("删除管理员, adminId: {}", id);

        // 检查管理员是否存在
        Admin admin = getById(id);
        if (admin == null) {
            log.warn("管理员不存在, adminId: {}", id);
            throw new AdminException.AdminNotFoundException(id);
        }

        boolean result = removeById(id);
        log.info("删除管理员成功, adminId: {}", id);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, allEntries = true)
    public boolean batchDeleteAdmins(List<Long> ids) {
        log.info("批量删除管理员, ids: {}", ids);

        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }

        boolean result = removeByIds(ids);
        log.info("批量删除管理员成功, count: {}", ids.size());
        return result;
    }

    // ================= 状态管理 =================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public boolean updateAdminStatus(Long id, Integer status) throws AdminException.AdminNotFoundException {
        log.info("更新管理员状态, adminId: {}, status: {}", id, status);

        Admin admin = getById(id);
        if (admin == null) {
            log.warn("管理员不存在, adminId: {}", id);
            throw new AdminException.AdminNotFoundException(id);
        }

        admin.setStatus(status);
        boolean result = updateById(admin);
        log.info("更新管理员状态成功, adminId: {}, status: {}", id, status);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public boolean enableAdmin(Long id) throws AdminException.AdminNotFoundException {
        log.info("启用管理员, adminId: {}", id);
        return updateAdminStatus(id, STATUS_ENABLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public boolean disableAdmin(Long id) throws AdminException.AdminNotFoundException {
        log.info("禁用管理员, adminId: {}", id);
        return updateAdminStatus(id, STATUS_DISABLED);
    }

    // ================= 密码管理 =================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public boolean resetPassword(Long id, String newPassword) throws AdminException.AdminNotFoundException {
        log.info("重置管理员密码, adminId: {}", id);

        if (!StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("新密码不能为空");
        }

        Admin admin = getById(id);
        if (admin == null) {
            log.warn("管理员不存在, adminId: {}", id);
            throw new AdminException.AdminNotFoundException(id);
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        boolean result = updateById(admin);
        log.info("重置管理员密码成功, adminId: {}", id);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public boolean changePassword(Long id, String oldPassword, String newPassword)
            throws AdminException.AdminNotFoundException, AdminException.AdminPasswordException {
        log.info("修改管理员密码, adminId: {}", id);

        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("密码不能为空");
        }

        Admin admin = getById(id);
        if (admin == null) {
            log.warn("管理员不存在, adminId: {}", id);
            throw new AdminException.AdminNotFoundException(id);
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            log.warn("旧密码错误, adminId: {}", id);
            throw new AdminException.AdminPasswordException("旧密码错误");
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        boolean result = updateById(admin);
        log.info("修改管理员密码成功, adminId: {}", id);
        return result;
    }

    // ================= 缓存管理 =================

    @Override
    @CacheEvict(cacheNames = ADMIN_CACHE, key = "#id")
    public void evictAdminCache(Long id) {
        log.info("清除管理员缓存, adminId: {}", id);
    }

    @Override
    @CacheEvict(cacheNames = ADMIN_CACHE, allEntries = true)
    public void evictAllAdminCache() {
        log.info("清除所有管理员缓存");
    }
}
