package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.user.exception.MerchantException;
import com.cloud.user.module.entity.Merchant;

import java.util.List;

/**
 * 商家服务接口
 * 提供商家相关的业务操作，包括CRUD、分页查询、状态管理、审核管理等
 *
 * @author what's up
 * @since 1.0.0
 */
public interface MerchantService extends IService<Merchant> {

    // ================= 查询操作 =================

    /**
     * 根据ID获取商家详情
     *
     * @param id 商家ID
     * @return 商家DTO
     * @throws MerchantException.MerchantNotFoundException 商家不存在异常
     */
    MerchantDTO getMerchantById(Long id) throws MerchantException.MerchantNotFoundException;

    /**
     * 根据用户名获取商家信息
     *
     * @param username 用户名
     * @return 商家DTO
     * @throws MerchantException.MerchantNotFoundException 商家不存在异常
     */
    MerchantDTO getMerchantByUsername(String username) throws MerchantException.MerchantNotFoundException;

    /**
     * 根据商家名称获取商家信息
     *
     * @param merchantName 商家名称
     * @return 商家DTO
     * @throws MerchantException.MerchantNotFoundException 商家不存在异常
     */
    MerchantDTO getMerchantByName(String merchantName) throws MerchantException.MerchantNotFoundException;

    /**
     * 根据ID列表批量获取商家
     *
     * @param ids 商家ID列表
     * @return 商家DTO列表
     */
    List<MerchantDTO> getMerchantsByIds(List<Long> ids);

    /**
     * 分页查询商家
     *
     * @param page   页码
     * @param size   每页数量
     * @param status 商家状态
     * @return 分页结果
     */
    Page<MerchantDTO> getMerchantsPage(Integer page, Integer size, Integer status);

    // ================= 创建和更新操作 =================

    /**
     * 创建商家（入驻申请）
     *
     * @param merchantDTO 商家信息
     * @return 创建的商家DTO
     * @throws MerchantException.MerchantAlreadyExistsException 商家已存在异常
     */
    MerchantDTO createMerchant(MerchantDTO merchantDTO) throws MerchantException.MerchantAlreadyExistsException;

    /**
     * 更新商家信息
     *
     * @param merchantDTO 商家信息
     * @return 是否更新成功
     * @throws MerchantException.MerchantNotFoundException 商家不存在异常
     */
    boolean updateMerchant(MerchantDTO merchantDTO) throws MerchantException.MerchantNotFoundException;

    /**
     * 删除商家
     *
     * @param id 商家ID
     * @return 是否删除成功
     * @throws MerchantException.MerchantNotFoundException 商家不存在异常
     */
    boolean deleteMerchant(Long id) throws MerchantException.MerchantNotFoundException;

    /**
     * 批量删除商家
     *
     * @param ids 商家ID列表
     * @return 是否删除成功
     */
    boolean batchDeleteMerchants(List<Long> ids);

    // ================= 状态管理 =================

    /**
     * 更新商家状态
     *
     * @param id     商家ID
     * @param status 状态
     * @return 是否更新成功
     * @throws MerchantException.MerchantNotFoundException 商家不存在异常
     */
    boolean updateMerchantStatus(Long id, Integer status) throws MerchantException.MerchantNotFoundException;

    /**
     * 启用商家
     *
     * @param id 商家ID
     * @return 是否启用成功
     * @throws MerchantException.MerchantNotFoundException 商家不存在异常
     */
    boolean enableMerchant(Long id) throws MerchantException.MerchantNotFoundException;

    /**
     * 禁用商家
     *
     * @param id 商家ID
     * @return 是否禁用成功
     * @throws MerchantException.MerchantNotFoundException 商家不存在异常
     */
    boolean disableMerchant(Long id) throws MerchantException.MerchantNotFoundException;

    // ================= 审核管理 =================

    /**
     * 审核通过商家
     *
     * @param id     商家ID
     * @param remark 审核备注
     * @return 是否审核成功
     * @throws MerchantException.MerchantNotFoundException 商家不存在异常
     * @throws MerchantException.MerchantAuditException    商家审核异常
     */
    boolean approveMerchant(Long id, String remark)
            throws MerchantException.MerchantNotFoundException, MerchantException.MerchantAuditException;

    /**
     * 拒绝商家申请
     *
     * @param id     商家ID
     * @param reason 拒绝原因
     * @return 是否拒绝成功
     * @throws MerchantException.MerchantNotFoundException 商家不存在异常
     * @throws MerchantException.MerchantAuditException    商家审核异常
     */
    boolean rejectMerchant(Long id, String reason)
            throws MerchantException.MerchantNotFoundException, MerchantException.MerchantAuditException;

    // ================= 统计信息 =================

    /**
     * 获取商家统计信息
     *
     * @param id 商家ID
     * @return 统计信息
     * @throws MerchantException.MerchantNotFoundException 商家不存在异常
     */
    Object getMerchantStatistics(Long id) throws MerchantException.MerchantNotFoundException;

    // ================= 缓存管理 =================

    /**
     * 清除商家缓存
     *
     * @param id 商家ID
     */
    void evictMerchantCache(Long id);

    /**
     * 清除所有商家缓存
     */
    void evictAllMerchantCache();
}
