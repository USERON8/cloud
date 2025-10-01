package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.user.exception.AdminException;
import com.cloud.user.module.entity.Admin;

import java.util.List;

/**
 * 管理员服务接口
 * 提供管理员相关的业务操作，包括CRUD、分页查询、状态管理、权限管理等
 *
 * @author what's up
 * @since 1.0.0
 */
public interface AdminService extends IService<Admin> {

    // ================= 查询操作 =================

    /**
     * 根据ID获取管理员详情
     *
     * @param id 管理员ID
     * @return 管理员DTO
     * @throws AdminException.AdminNotFoundException 管理员不存在异常
     */
    AdminDTO getAdminById(Long id) throws AdminException.AdminNotFoundException;

    /**
     * 根据用户名获取管理员信息
     *
     * @param username 用户名
     * @return 管理员DTO
     * @throws AdminException.AdminNotFoundException 管理员不存在异常
     */
    AdminDTO getAdminByUsername(String username) throws AdminException.AdminNotFoundException;

    /**
     * 根据ID列表批量获取管理员
     *
     * @param ids 管理员ID列表
     * @return 管理员DTO列表
     */
    List<AdminDTO> getAdminsByIds(List<Long> ids);

    /**
     * 分页查询管理员
     *
     * @param page   页码
     * @param size   每页数量
     * @param status 管理员状态
     * @return 分页结果
     */
    Page<AdminDTO> getMerchantsPage(Integer page, Integer size, Integer status);

    // ================= 创建和更新操作 =================

    /**
     * 创建管理员
     *
     * @param adminDTO 管理员信息
     * @return 创建的管理员DTO
     * @throws AdminException.AdminAlreadyExistsException 管理员已存在异常
     */
    AdminDTO createAdmin(AdminDTO adminDTO) throws AdminException.AdminAlreadyExistsException;

    /**
     * 更新管理员信息
     *
     * @param adminDTO 管理员信息
     * @return 是否更新成功
     * @throws AdminException.AdminNotFoundException 管理员不存在异常
     */
    boolean updateAdmin(AdminDTO adminDTO) throws AdminException.AdminNotFoundException;

    /**
     * 删除管理员
     *
     * @param id 管理员ID
     * @return 是否删除成功
     * @throws AdminException.AdminNotFoundException 管理员不存在异常
     */
    boolean deleteAdmin(Long id) throws AdminException.AdminNotFoundException;

    /**
     * 批量删除管理员
     *
     * @param ids 管理员ID列表
     * @return 是否删除成功
     */
    boolean batchDeleteAdmins(List<Long> ids);

    // ================= 状态管理 =================

    /**
     * 更新管理员状态
     *
     * @param id     管理员ID
     * @param status 状态
     * @return 是否更新成功
     * @throws AdminException.AdminNotFoundException 管理员不存在异常
     */
    boolean updateAdminStatus(Long id, Integer status) throws AdminException.AdminNotFoundException;

    /**
     * 启用管理员
     *
     * @param id 管理员ID
     * @return 是否启用成功
     * @throws AdminException.AdminNotFoundException 管理员不存在异常
     */
    boolean enableAdmin(Long id) throws AdminException.AdminNotFoundException;

    /**
     * 禁用管理员
     *
     * @param id 管理员ID
     * @return 是否禁用成功
     * @throws AdminException.AdminNotFoundException 管理员不存在异常
     */
    boolean disableAdmin(Long id) throws AdminException.AdminNotFoundException;

    // ================= 密码管理 =================

    /**
     * 重置管理员密码
     *
     * @param id          管理员ID
     * @param newPassword 新密码
     * @return 是否重置成功
     * @throws AdminException.AdminNotFoundException 管理员不存在异常
     */
    boolean resetPassword(Long id, String newPassword) throws AdminException.AdminNotFoundException;

    /**
     * 修改管理员密码
     *
     * @param id          管理员ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否修改成功
     * @throws AdminException.AdminNotFoundException  管理员不存在异常
     * @throws AdminException.AdminPasswordException 密码错误异常
     */
    boolean changePassword(Long id, String oldPassword, String newPassword)
            throws AdminException.AdminNotFoundException, AdminException.AdminPasswordException;

    // ================= 缓存管理 =================

    /**
     * 清除管理员缓存
     *
     * @param id 管理员ID
     */
    void evictAdminCache(Long id);

    /**
     * 清除所有管理员缓存
     */
    void evictAllAdminCache();
}
