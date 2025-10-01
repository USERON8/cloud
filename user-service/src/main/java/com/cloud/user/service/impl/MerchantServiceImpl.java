package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.user.converter.MerchantConverter;
import com.cloud.user.exception.MerchantException;
import com.cloud.user.mapper.MerchantMapper;
import com.cloud.user.module.entity.Merchant;
import com.cloud.user.service.MerchantService;
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
 * 商家服务实现类
 * 提供商家相关的业务操作实现，包含分布式锁、缓存、事务等处理
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements MerchantService {

    // 缓存名称
    private static final String MERCHANT_CACHE = "merchant";

    // 商家状态
    private static final Integer STATUS_PENDING = 0;    // 待审核
    private static final Integer STATUS_APPROVED = 1;   // 审核通过
    private static final Integer STATUS_REJECTED = 2;   // 审核拒绝
    private static final Integer STATUS_ENABLED = 1;    // 启用
    private static final Integer STATUS_DISABLED = 0;   // 禁用

    private final MerchantMapper merchantMapper;
    private final MerchantConverter merchantConverter;
    private final PasswordEncoder passwordEncoder;

    // ================= 查询操作 =================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = MERCHANT_CACHE, key = "#id", unless = "#result == null")
    public MerchantDTO getMerchantById(Long id) throws MerchantException.MerchantNotFoundException {
        log.info("查询商家信息, merchantId: {}", id);

        Merchant merchant = getById(id);
        if (merchant == null) {
            log.warn("商家不存在, merchantId: {}", id);
            throw new MerchantException.MerchantNotFoundException(id);
        }

        return merchantConverter.toDTO(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = MERCHANT_CACHE, key = "'username:' + #username", unless = "#result == null")
    public MerchantDTO getMerchantByUsername(String username) throws MerchantException.MerchantNotFoundException {
        log.info("根据用户名查询商家, username: {}", username);

        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        Merchant merchant = lambdaQuery()
                .eq(Merchant::getUsername, username)
                .one();

        if (merchant == null) {
            log.warn("商家不存在, username: {}", username);
            throw new MerchantException.MerchantNotFoundException(username);
        }

        return merchantConverter.toDTO(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = MERCHANT_CACHE, key = "'name:' + #merchantName", unless = "#result == null")
    public MerchantDTO getMerchantByName(String merchantName) throws MerchantException.MerchantNotFoundException {
        log.info("根据商家名称查询, merchantName: {}", merchantName);

        if (!StringUtils.hasText(merchantName)) {
            throw new IllegalArgumentException("商家名称不能为空");
        }

        Merchant merchant = lambdaQuery()
                .eq(Merchant::getMerchantName, merchantName)
                .one();

        if (merchant == null) {
            log.warn("商家不存在, merchantName: {}", merchantName);
            throw new MerchantException.MerchantNotFoundException(merchantName);
        }

        return merchantConverter.toDTO(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MerchantDTO> getMerchantsByIds(List<Long> ids) {
        log.info("批量查询商家, ids: {}", ids);

        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }

        List<Merchant> merchants = listByIds(ids);
        return merchants.stream()
                .map(merchantConverter::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MerchantDTO> getMerchantsPage(Integer page, Integer size, Integer status) {
        log.info("分页查询商家, page: {}, size: {}, status: {}", page, size, status);

        Page<Merchant> pageParam = new Page<>(page, size);
        Page<Merchant> merchantPage = lambdaQuery()
                .eq(status != null, Merchant::getStatus, status)
                .orderByDesc(Merchant::getCreatedAt)
                .page(pageParam);

        Page<MerchantDTO> dtoPage = new Page<>(merchantPage.getCurrent(), merchantPage.getSize(), merchantPage.getTotal());
        List<MerchantDTO> dtoList = merchantPage.getRecords().stream()
                .map(merchantConverter::toDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    // ================= 创建和更新操作 =================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CachePut(cacheNames = MERCHANT_CACHE, key = "#result.id")
    @DistributedLock(
            key = "'create:' + #merchantDTO.username",
            prefix = "merchant",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "创建商家失败，请稍后重试"
    )
    public MerchantDTO createMerchant(MerchantDTO merchantDTO) throws MerchantException.MerchantAlreadyExistsException {
        log.info("创建商家（入驻申请）, username: {}", merchantDTO.getUsername());

        // 检查用户名是否已存在
        long usernameCount = lambdaQuery()
                .eq(Merchant::getUsername, merchantDTO.getUsername())
                .count();

        if (usernameCount > 0) {
            log.warn("商家用户名已存在, username: {}", merchantDTO.getUsername());
            throw new MerchantException.MerchantAlreadyExistsException(merchantDTO.getUsername());
        }

        // 检查商家名称是否已存在
        if (StringUtils.hasText(merchantDTO.getMerchantName())) {
            long nameCount = lambdaQuery()
                    .eq(Merchant::getMerchantName, merchantDTO.getMerchantName())
                    .count();

            if (nameCount > 0) {
                log.warn("商家名称已存在, merchantName: {}", merchantDTO.getMerchantName());
                throw new MerchantException.MerchantAlreadyExistsException(merchantDTO.getMerchantName());
            }
        }

        // 转换并保存
        Merchant merchant = merchantConverter.toEntity(merchantDTO);

        // 加密密码
        if (StringUtils.hasText(merchant.getPassword())) {
            merchant.setPassword(passwordEncoder.encode(merchant.getPassword()));
        }

        // 设置默认状态为待审核
        if (merchant.getStatus() == null) {
            merchant.setStatus(STATUS_PENDING);
        }

        if (!save(merchant)) {
            throw new MerchantException("创建商家失败");
        }

        log.info("创建商家成功, merchantId: {}", merchant.getId());
        return merchantConverter.toDTO(merchant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#merchantDTO.id"),
            @CacheEvict(cacheNames = MERCHANT_CACHE, key = "'username:' + #merchantDTO.username")
    })
    @DistributedLock(
            key = "'update:' + #merchantDTO.id",
            prefix = "merchant",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "更新商家失败，请稍后重试"
    )
    public boolean updateMerchant(MerchantDTO merchantDTO) throws MerchantException.MerchantNotFoundException {
        log.info("更新商家信息, merchantId: {}", merchantDTO.getId());

        // 检查商家是否存在
        Merchant existingMerchant = getById(merchantDTO.getId());
        if (existingMerchant == null) {
            log.warn("商家不存在, merchantId: {}", merchantDTO.getId());
            throw new MerchantException.MerchantNotFoundException(merchantDTO.getId());
        }

        // 如果更新用户名，检查新用户名是否被占用
        if (StringUtils.hasText(merchantDTO.getUsername()) &&
                !merchantDTO.getUsername().equals(existingMerchant.getUsername())) {

            long count = lambdaQuery()
                    .eq(Merchant::getUsername, merchantDTO.getUsername())
                    .ne(Merchant::getId, merchantDTO.getId())
                    .count();

            if (count > 0) {
                throw new MerchantException.MerchantAlreadyExistsException(merchantDTO.getUsername());
            }
        }

        // 如果更新商家名称，检查是否被占用
        if (StringUtils.hasText(merchantDTO.getMerchantName()) &&
                !merchantDTO.getMerchantName().equals(existingMerchant.getMerchantName())) {

            long count = lambdaQuery()
                    .eq(Merchant::getMerchantName, merchantDTO.getMerchantName())
                    .ne(Merchant::getId, merchantDTO.getId())
                    .count();

            if (count > 0) {
                throw new MerchantException.MerchantAlreadyExistsException(merchantDTO.getMerchantName());
            }
        }

        // 转换并更新
        Merchant merchant = merchantConverter.toEntity(merchantDTO);
        merchant.setId(merchantDTO.getId());

        // 如果包含密码，则加密
        if (StringUtils.hasText(merchant.getPassword())) {
            merchant.setPassword(passwordEncoder.encode(merchant.getPassword()));
        } else {
            merchant.setPassword(null); // 不更新密码
        }

        boolean result = updateById(merchant);
        log.info("更新商家成功, merchantId: {}", merchantDTO.getId());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    @DistributedLock(
            key = "'delete:' + #id",
            prefix = "merchant",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "删除商家失败，请稍后重试"
    )
    public boolean deleteMerchant(Long id) throws MerchantException.MerchantNotFoundException {
        log.info("删除商家, merchantId: {}", id);

        // 检查商家是否存在
        Merchant merchant = getById(id);
        if (merchant == null) {
            log.warn("商家不存在, merchantId: {}", id);
            throw new MerchantException.MerchantNotFoundException(id);
        }

        boolean result = removeById(id);
        log.info("删除商家成功, merchantId: {}", id);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, allEntries = true)
    public boolean batchDeleteMerchants(List<Long> ids) {
        log.info("批量删除商家, ids: {}", ids);

        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }

        boolean result = removeByIds(ids);
        log.info("批量删除商家成功, count: {}", ids.size());
        return result;
    }

    // ================= 状态管理 =================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    public boolean updateMerchantStatus(Long id, Integer status) throws MerchantException.MerchantNotFoundException {
        log.info("更新商家状态, merchantId: {}, status: {}", id, status);

        Merchant merchant = getById(id);
        if (merchant == null) {
            log.warn("商家不存在, merchantId: {}", id);
            throw new MerchantException.MerchantNotFoundException(id);
        }

        merchant.setStatus(status);
        boolean result = updateById(merchant);
        log.info("更新商家状态成功, merchantId: {}, status: {}", id, status);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    public boolean enableMerchant(Long id) throws MerchantException.MerchantNotFoundException {
        log.info("启用商家, merchantId: {}", id);
        return updateMerchantStatus(id, STATUS_ENABLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    public boolean disableMerchant(Long id) throws MerchantException.MerchantNotFoundException {
        log.info("禁用商家, merchantId: {}", id);
        return updateMerchantStatus(id, STATUS_DISABLED);
    }

    // ================= 审核管理 =================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    @DistributedLock(
            key = "'approve:' + #id",
            prefix = "merchant",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "审核商家失败，请稍后重试"
    )
    public boolean approveMerchant(Long id, String remark)
            throws MerchantException.MerchantNotFoundException, MerchantException.MerchantAuditException {
        log.info("审核通过商家, merchantId: {}, remark: {}", id, remark);

        Merchant merchant = getById(id);
        if (merchant == null) {
            log.warn("商家不存在, merchantId: {}", id);
            throw new MerchantException.MerchantNotFoundException(id);
        }

        // 检查当前状态是否为待审核
        if (!STATUS_PENDING.equals(merchant.getStatus())) {
            log.warn("商家状态不是待审核，无法审核, merchantId: {}, status: {}", id, merchant.getStatus());
            throw new MerchantException.MerchantAuditException("商家状态不是待审核，无法审核");
        }

        merchant.setStatus(STATUS_APPROVED);
        boolean result = updateById(merchant);
        log.info("审核通过商家成功, merchantId: {}", id);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    @DistributedLock(
            key = "'reject:' + #id",
            prefix = "merchant",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "拒绝商家申请失败，请稍后重试"
    )
    public boolean rejectMerchant(Long id, String reason)
            throws MerchantException.MerchantNotFoundException, MerchantException.MerchantAuditException {
        log.info("拒绝商家申请, merchantId: {}, reason: {}", id, reason);

        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("拒绝原因不能为空");
        }

        Merchant merchant = getById(id);
        if (merchant == null) {
            log.warn("商家不存在, merchantId: {}", id);
            throw new MerchantException.MerchantNotFoundException(id);
        }

        // 检查当前状态是否为待审核
        if (!STATUS_PENDING.equals(merchant.getStatus())) {
            log.warn("商家状态不是待审核，无法拒绝, merchantId: {}, status: {}", id, merchant.getStatus());
            throw new MerchantException.MerchantAuditException("商家状态不是待审核，无法拒绝");
        }

        merchant.setStatus(STATUS_REJECTED);
        boolean result = updateById(merchant);
        log.info("拒绝商家申请成功, merchantId: {}", id);
        return result;
    }

    // ================= 统计信息 =================

    @Override
    @Transactional(readOnly = true)
    public Object getMerchantStatistics(Long id) throws MerchantException.MerchantNotFoundException {
        log.info("获取商家统计信息, merchantId: {}", id);

        Merchant merchant = getById(id);
        if (merchant == null) {
            log.warn("商家不存在, merchantId: {}", id);
            throw new MerchantException.MerchantNotFoundException(id);
        }

        // TODO: 实现具体的统计逻辑，可能需要调用其他服务获取商品数、订单数等统计信息
        // 这里返回一个简单的Map作为示例
        return java.util.Map.of(
                "merchantId", id,
                "merchantName", merchant.getMerchantName(),
                "status", merchant.getStatus(),
                "createdAt", merchant.getCreatedAt()
                // 可以添加更多统计信息
        );
    }

    // ================= 缓存管理 =================

    @Override
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    public void evictMerchantCache(Long id) {
        log.info("清除商家缓存, merchantId: {}", id);
    }

    @Override
    @CacheEvict(cacheNames = MERCHANT_CACHE, allEntries = true)
    public void evictAllMerchantCache() {
        log.info("清除所有商家缓存");
    }
}
