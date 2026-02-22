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
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements MerchantService {

    private static final String MERCHANT_CACHE = "merchant";
    private static final Integer STATUS_PENDING = 0;
    private static final Integer STATUS_APPROVED = 1;
    private static final Integer STATUS_REJECTED = 2;
    private static final Integer STATUS_ENABLED = 1;
    private static final Integer STATUS_DISABLED = 0;

    private final MerchantMapper merchantMapper;
    private final MerchantConverter merchantConverter;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = MERCHANT_CACHE, key = "#id", unless = "#result == null")
    public MerchantDTO getMerchantById(Long id) throws MerchantException.MerchantNotFoundException {
        Merchant merchant = getById(id);
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(id);
        }
        return merchantConverter.toDTO(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = MERCHANT_CACHE, key = "'username:' + #username", unless = "#result == null")
    public MerchantDTO getMerchantByUsername(String username) throws MerchantException.MerchantNotFoundException {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("username is required");
        }

        Merchant merchant = lambdaQuery().eq(Merchant::getUsername, username).one();
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(username);
        }
        return merchantConverter.toDTO(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = MERCHANT_CACHE, key = "'name:' + #merchantName", unless = "#result == null")
    public MerchantDTO getMerchantByName(String merchantName) throws MerchantException.MerchantNotFoundException {
        if (!StringUtils.hasText(merchantName)) {
            throw new IllegalArgumentException("merchantName is required");
        }

        Merchant merchant = lambdaQuery().eq(Merchant::getMerchantName, merchantName).one();
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(merchantName);
        }
        return merchantConverter.toDTO(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MerchantDTO> getMerchantsByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return listByIds(ids).stream().map(merchantConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MerchantDTO> getMerchantsPage(Integer page, Integer size, Integer status) {
        Page<Merchant> pageParam = new Page<>(page, size);
        Page<Merchant> merchantPage = lambdaQuery()
                .eq(status != null, Merchant::getStatus, status)
                .orderByDesc(Merchant::getCreatedAt)
                .page(pageParam);

        Page<MerchantDTO> dtoPage = new Page<>(merchantPage.getCurrent(), merchantPage.getSize(), merchantPage.getTotal());
        dtoPage.setRecords(merchantPage.getRecords().stream().map(merchantConverter::toDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CachePut(cacheNames = MERCHANT_CACHE, key = "#result.id")
    @DistributedLock(
            key = "'create:' + #merchantDTO.username",
            prefix = "merchant",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "failed to acquire create merchant lock"
    )
    public MerchantDTO createMerchant(MerchantDTO merchantDTO) throws MerchantException.MerchantAlreadyExistsException {
        if (lambdaQuery().eq(Merchant::getUsername, merchantDTO.getUsername()).count() > 0) {
            throw new MerchantException.MerchantAlreadyExistsException(merchantDTO.getUsername());
        }

        if (StringUtils.hasText(merchantDTO.getMerchantName())) {
            if (lambdaQuery().eq(Merchant::getMerchantName, merchantDTO.getMerchantName()).count() > 0) {
                throw new MerchantException.MerchantAlreadyExistsException(merchantDTO.getMerchantName());
            }
        }

        Merchant merchant = merchantConverter.toEntity(merchantDTO);
        if (StringUtils.hasText(merchant.getPassword())) {
            merchant.setPassword(passwordEncoder.encode(merchant.getPassword()));
        }
        if (merchant.getStatus() == null) {
            merchant.setStatus(STATUS_PENDING);
        }

        if (!save(merchant)) {
            throw new MerchantException("failed to create merchant");
        }
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
            failMessage = "failed to acquire update merchant lock"
    )
    public boolean updateMerchant(MerchantDTO merchantDTO) throws MerchantException.MerchantNotFoundException {
        Merchant existing = getById(merchantDTO.getId());
        if (existing == null) {
            throw new MerchantException.MerchantNotFoundException(merchantDTO.getId());
        }

        if (StringUtils.hasText(merchantDTO.getUsername()) && !merchantDTO.getUsername().equals(existing.getUsername())) {
            long count = lambdaQuery()
                    .eq(Merchant::getUsername, merchantDTO.getUsername())
                    .ne(Merchant::getId, merchantDTO.getId())
                    .count();
            if (count > 0) {
                throw new MerchantException.MerchantAlreadyExistsException(merchantDTO.getUsername());
            }
        }

        if (StringUtils.hasText(merchantDTO.getMerchantName()) && !merchantDTO.getMerchantName().equals(existing.getMerchantName())) {
            long count = lambdaQuery()
                    .eq(Merchant::getMerchantName, merchantDTO.getMerchantName())
                    .ne(Merchant::getId, merchantDTO.getId())
                    .count();
            if (count > 0) {
                throw new MerchantException.MerchantAlreadyExistsException(merchantDTO.getMerchantName());
            }
        }

        Merchant merchant = merchantConverter.toEntity(merchantDTO);
        merchant.setId(merchantDTO.getId());
        if (StringUtils.hasText(merchant.getPassword())) {
            merchant.setPassword(passwordEncoder.encode(merchant.getPassword()));
        } else {
            merchant.setPassword(null);
        }

        return updateById(merchant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    @DistributedLock(
            key = "'delete:' + #id",
            prefix = "merchant",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "failed to acquire delete merchant lock"
    )
    public boolean deleteMerchant(Long id) throws MerchantException.MerchantNotFoundException {
        Merchant merchant = getById(id);
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(id);
        }
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, allEntries = true)
    public boolean batchDeleteMerchants(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }
        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    public boolean updateMerchantStatus(Long id, Integer status) throws MerchantException.MerchantNotFoundException {
        Merchant merchant = getById(id);
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(id);
        }
        merchant.setStatus(status);
        return updateById(merchant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    public boolean enableMerchant(Long id) throws MerchantException.MerchantNotFoundException {
        return updateMerchantStatus(id, STATUS_ENABLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    public boolean disableMerchant(Long id) throws MerchantException.MerchantNotFoundException {
        return updateMerchantStatus(id, STATUS_DISABLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    @DistributedLock(
            key = "'approve:' + #id",
            prefix = "merchant",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "failed to acquire approve merchant lock"
    )
    public boolean approveMerchant(Long id, String remark)
            throws MerchantException.MerchantNotFoundException, MerchantException.MerchantAuditException {
        Merchant merchant = getById(id);
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(id);
        }
        if (!STATUS_PENDING.equals(merchant.getStatus())) {
            throw new MerchantException.MerchantAuditException("merchant status is not pending");
        }
        merchant.setStatus(STATUS_APPROVED);
        return updateById(merchant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    @DistributedLock(
            key = "'reject:' + #id",
            prefix = "merchant",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "failed to acquire reject merchant lock"
    )
    public boolean rejectMerchant(Long id, String reason)
            throws MerchantException.MerchantNotFoundException, MerchantException.MerchantAuditException {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("reason is required");
        }

        Merchant merchant = getById(id);
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(id);
        }
        if (!STATUS_PENDING.equals(merchant.getStatus())) {
            throw new MerchantException.MerchantAuditException("merchant status is not pending");
        }
        merchant.setStatus(STATUS_REJECTED);
        return updateById(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getMerchantStatistics(Long id) throws MerchantException.MerchantNotFoundException {
        Merchant merchant = getById(id);
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(id);
        }

        return Map.of(
                "merchantId", id,
                "merchantName", merchant.getMerchantName(),
                "status", merchant.getStatus(),
                "createdAt", merchant.getCreatedAt()
        );
    }

    @Override
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    public void evictMerchantCache(Long id) {
    }

    @Override
    @CacheEvict(cacheNames = MERCHANT_CACHE, allEntries = true)
    public void evictAllMerchantCache() {
    }
}