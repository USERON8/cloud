package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.user.annotation.MultiLevelCacheEvict;
import com.cloud.user.annotation.MultiLevelCachePut;
import com.cloud.user.annotation.MultiLevelCacheable;
import com.cloud.user.converter.AdminConverter;
import com.cloud.user.mapper.AdminMapper;
import com.cloud.user.module.entity.Admin;
import com.cloud.user.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author what's up
 * @description 针对表【admin(管理员表)】的数据库操作Service实现
 * @createDate 2025-09-06 19:31:12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin>
        implements AdminService {

    // 管理员缓存名称
    private static final String ADMIN_CACHE_NAME = "adminCache";
    private final AdminMapper adminMapper;
    private final AdminConverter adminConverter;

    /**
     * 根据ID获取管理员信息(带缓存)
     *
     * @param id 管理员ID
     * @return 管理员DTO
     */
    @Transactional(readOnly = true)
    @MultiLevelCacheable(
            cacheName = ADMIN_CACHE_NAME,
            key = "#id",
            expire = 1800,
            unless = "#result == null"
    )
    public AdminDTO getAdminById(Long id) {
        Admin admin = getById(id);
        return admin != null ? adminConverter.toDTO(admin) : null;
    }

    /**
     * 根据用户名获取管理员信息(带缓存)
     *
     * @param username 用户名
     * @return 管理员DTO
     */
    @Transactional(readOnly = true)
    @MultiLevelCacheable(
            cacheName = ADMIN_CACHE_NAME,
            key = "'username:' + #username",
            expire = 1800,
            unless = "#result == null"
    )
    public AdminDTO getAdminByUsername(String username) {
        Admin admin = lambdaQuery().eq(Admin::getUsername, username).one();
        return admin != null ? adminConverter.toDTO(admin) : null;
    }

    /**
     * 更新管理员信息(同时清理缓存)
     *
     * @param admin 管理员实体
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCacheEvict(
            cacheName = ADMIN_CACHE_NAME,
            key = "#admin.id"
    )
    public boolean updateById(Admin admin) {
        log.info("更新管理员信息, adminId: {}", admin.getId());
        return super.updateById(admin);
    }

    /**
     * 删除管理员(同时清理缓存)
     *
     * @param id 管理员ID
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCacheEvict(
            cacheName = ADMIN_CACHE_NAME,
            key = "#id"
    )
    public boolean removeById(java.io.Serializable id) {
        log.info("删除管理员, adminId: {}", id);
        return super.removeById(id);
    }

    /**
     * 保存管理员信息
     *
     * @param admin 管理员实体
     * @return 保存结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @MultiLevelCachePut(
            cacheName = ADMIN_CACHE_NAME,
            key = "#admin.id",
            expire = 1800
    )
    public boolean save(Admin admin) {
        log.info("保存管理员信息, username: {}", admin.getUsername());
        return super.save(admin);
    }
}

