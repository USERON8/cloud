package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.common.domain.dto.user.MerchantUpsertRequestDTO;
import com.cloud.user.converter.MerchantConverter;
import com.cloud.user.exception.MerchantException;
import com.cloud.user.mapper.MerchantAuthMapper;
import com.cloud.user.mapper.MerchantMapper;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.Merchant;
import com.cloud.user.module.entity.MerchantAuth;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.MerchantService;
import com.cloud.user.service.support.AuthPrincipalRemoteService;
import com.cloud.user.service.support.UserPrincipalSyncService;
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
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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

    private final MerchantAuthMapper merchantAuthMapper;
    private final UserMapper userMapper;
    private final MerchantConverter merchantConverter;
    private final AuthPrincipalRemoteService authPrincipalRemoteService;
    private final UserPrincipalSyncService userPrincipalSyncService;
    private final CacheManager cacheManager;

    @Override
    @Transactional(readOnly = true)
    public MerchantDTO getMerchantById(Long id) throws MerchantException.MerchantNotFoundException {
        Merchant merchant = getById(id);
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(id);
        }
        return toEnrichedDTO(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantDTO getMerchantByUsername(String username) throws MerchantException.MerchantNotFoundException {
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("username is required");
        }

        Merchant merchant = lambdaQuery().eq(Merchant::getUsername, username).one();
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(username);
        }
        return toEnrichedDTO(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantDTO getMerchantByName(String merchantName) throws MerchantException.MerchantNotFoundException {
        if (StrUtil.isBlank(merchantName)) {
            throw new IllegalArgumentException("merchantName is required");
        }

        Merchant merchant = lambdaQuery().eq(Merchant::getMerchantName, merchantName).one();
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(merchantName);
        }
        return toEnrichedDTO(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MerchantDTO> getMerchantsByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return toEnrichedDTOList(listByIds(ids));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MerchantDTO> getMerchantsPage(Integer page, Integer size, Integer status, Integer auditStatus) {
        Page<Merchant> pageParam = new Page<>(page, size);
        Page<Merchant> merchantPage = lambdaQuery()
                .eq(status != null, Merchant::getStatus, status)
                .eq(auditStatus != null, Merchant::getAuditStatus, auditStatus)
                .orderByDesc(Merchant::getCreatedAt)
                .page(pageParam);

        Page<MerchantDTO> dtoPage = new Page<>(merchantPage.getCurrent(), merchantPage.getSize(), merchantPage.getTotal());
        dtoPage.setRecords(toEnrichedDTOList(merchantPage.getRecords()));
        return dtoPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CachePut(cacheNames = MERCHANT_CACHE, key = "#result.id")
    @DistributedLock(
            key = "'create:' + #requestDTO.username",
            prefix = "merchant",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "failed to acquire create merchant lock"
    )
    public MerchantDTO createMerchant(MerchantUpsertRequestDTO requestDTO) throws MerchantException.MerchantAlreadyExistsException {
        if (StrUtil.isBlank(requestDTO.getUsername())) {
            throw new IllegalArgumentException("username is required");
        }
        if (StrUtil.isBlank(requestDTO.getMerchantName())) {
            throw new IllegalArgumentException("merchantName is required");
        }
        if (StrUtil.isBlank(requestDTO.getPassword())) {
            throw new IllegalArgumentException("password is required");
        }

        if (lambdaQuery().eq(Merchant::getUsername, requestDTO.getUsername()).count() > 0) {
            throw new MerchantException.MerchantAlreadyExistsException(requestDTO.getUsername());
        }
        authPrincipalRemoteService.assertUsernameAvailable(requestDTO.getUsername(), null);

        if (StrUtil.isNotBlank(requestDTO.getMerchantName())) {
            if (lambdaQuery().eq(Merchant::getMerchantName, requestDTO.getMerchantName()).count() > 0) {
                throw new MerchantException.MerchantAlreadyExistsException(requestDTO.getMerchantName());
            }
        }

        Merchant merchant = toMerchantEntity(requestDTO);
        if (merchant.getStatus() == null) {
            merchant.setStatus(STATUS_ENABLED);
        }
        if (merchant.getAuditStatus() == null) {
            merchant.setAuditStatus(STATUS_PENDING);
        }

        if (!save(merchant)) {
            throw new MerchantException("failed to create merchant");
        }
        userPrincipalSyncService.upsertUserPrincipal(
                merchant.getId(),
                merchant.getUsername(),
                merchant.getMerchantName(),
                requestDTO.getEmail(),
                merchant.getPhone(),
                merchant.getStatus()
        );
        authPrincipalRemoteService.createPrincipal(toAuthPrincipalDTO(merchant, requestDTO.getEmail(), requestDTO.getPassword()));
        return toEnrichedDTO(merchant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id"),
            @CacheEvict(cacheNames = MERCHANT_CACHE, key = "'username:' + #requestDTO.username")
    })
    @DistributedLock(
            key = "'update:' + #id",
            prefix = "merchant",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "failed to acquire update merchant lock"
    )
    public boolean updateMerchant(Long id, MerchantUpsertRequestDTO requestDTO) throws MerchantException.MerchantNotFoundException {
        if (id == null || requestDTO == null) {
            throw new IllegalArgumentException("merchant id is required");
        }
        Merchant existing = getById(id);
        if (existing == null) {
            throw new MerchantException.MerchantNotFoundException(id);
        }

        if (StrUtil.isNotBlank(requestDTO.getUsername()) && !requestDTO.getUsername().equals(existing.getUsername())) {
            long count = lambdaQuery()
                    .eq(Merchant::getUsername, requestDTO.getUsername())
                    .ne(Merchant::getId, id)
                    .count();
            if (count > 0) {
                throw new MerchantException.MerchantAlreadyExistsException(requestDTO.getUsername());
            }
            authPrincipalRemoteService.assertUsernameAvailable(requestDTO.getUsername(), id);
        }

        if (StrUtil.isNotBlank(requestDTO.getMerchantName()) && !requestDTO.getMerchantName().equals(existing.getMerchantName())) {
            long count = lambdaQuery()
                    .eq(Merchant::getMerchantName, requestDTO.getMerchantName())
                    .ne(Merchant::getId, id)
                    .count();
            if (count > 0) {
                throw new MerchantException.MerchantAlreadyExistsException(requestDTO.getMerchantName());
            }
        }

        Merchant merchant = toMerchantEntity(requestDTO);
        merchant.setId(id);
        if (StrUtil.isBlank(merchant.getUsername())) {
            merchant.setUsername(existing.getUsername());
        }
        if (StrUtil.isBlank(merchant.getMerchantName())) {
            merchant.setMerchantName(existing.getMerchantName());
        }
        if (StrUtil.isBlank(merchant.getPhone())) {
            merchant.setPhone(existing.getPhone());
        }
        if (merchant.getStatus() == null) {
            merchant.setStatus(existing.getStatus());
        }
        if (merchant.getAuditStatus() == null) {
            merchant.setAuditStatus(existing.getAuditStatus());
        }
        boolean updated = updateById(merchant);
        if (updated) {
            Merchant current = resolveCurrentMerchant(requestDTO, existing);
            userPrincipalSyncService.upsertUserPrincipal(
                    current.getId(),
                    current.getUsername(),
                    current.getMerchantName(),
                    requestDTO.getEmail(),
                    current.getPhone(),
                    current.getStatus()
            );
            authPrincipalRemoteService.updatePrincipal(toAuthPrincipalDTO(current, requestDTO.getEmail(), requestDTO.getPassword()));
            evictUsernameCacheIfChanged(existing.getUsername(), requestDTO.getUsername());
        }
        return updated;
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
        boolean removed = removeById(id);
        if (removed) {
            userPrincipalSyncService.deleteUserPrincipal(id);
            authPrincipalRemoteService.deletePrincipal(id);
        }
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, allEntries = true)
    public boolean batchDeleteMerchants(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }
        boolean removed = removeByIds(ids);
        if (removed) {
            ids.forEach(id -> {
                userPrincipalSyncService.deleteUserPrincipal(id);
                authPrincipalRemoteService.deletePrincipal(id);
            });
        }
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    public boolean updateMerchantStatus(Long id, Integer status) throws MerchantException.MerchantNotFoundException {
        Merchant merchant = getById(id);
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(id);
        }
        if (!isValidEnableStatus(status)) {
            throw new MerchantException.MerchantStatusException(id, String.valueOf(status));
        }
        merchant.setStatus(status);
        boolean updated = updateById(merchant);
        if (updated) {
            Merchant refreshed = getById(id);
            if (refreshed != null) {
                MerchantDTO merchantDTO = toEnrichedDTO(refreshed);
                userPrincipalSyncService.upsertUserPrincipal(
                        refreshed.getId(),
                        refreshed.getUsername(),
                        refreshed.getMerchantName(),
                        merchantDTO.getEmail(),
                        refreshed.getPhone(),
                        refreshed.getStatus()
                );
                AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
                authPrincipalDTO.setId(refreshed.getId());
                authPrincipalDTO.setStatus(refreshed.getStatus());
                authPrincipalRemoteService.updatePrincipal(authPrincipalDTO);
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = MERCHANT_CACHE, key = "#id")
    @DistributedLock(
            key = "'audit:' + #id",
            prefix = "merchant",
            waitTime = 10,
            leaseTime = 30,
            failMessage = "failed to acquire audit merchant lock"
    )
    public boolean updateMerchantAuditStatus(Long id, Integer auditStatus) throws MerchantException.MerchantNotFoundException {
        Merchant merchant = getById(id);
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(id);
        }
        if (!isValidAuditStatus(auditStatus)) {
            throw new MerchantException.MerchantAuditException("invalid audit status: " + auditStatus);
        }
        merchant.setAuditStatus(auditStatus);
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
        if (!STATUS_PENDING.equals(merchant.getAuditStatus())) {
            throw new MerchantException.MerchantAuditException("merchant status is not pending");
        }
        return updateMerchantAuditStatus(id, STATUS_APPROVED);
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
        if (StrUtil.isBlank(reason)) {
            throw new IllegalArgumentException("reason is required");
        }

        Merchant merchant = getById(id);
        if (merchant == null) {
            throw new MerchantException.MerchantNotFoundException(id);
        }
        if (!STATUS_PENDING.equals(merchant.getAuditStatus())) {
            throw new MerchantException.MerchantAuditException("merchant status is not pending");
        }
        return updateMerchantAuditStatus(id, STATUS_REJECTED);
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
                "auditStatus", merchant.getAuditStatus(),
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

    private MerchantDTO toEnrichedDTO(Merchant merchant) {
        MerchantDTO merchantDTO = merchantConverter.toDTO(merchant);

        MerchantAuth merchantAuth = merchantAuthMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MerchantAuth>()
                        .eq(MerchantAuth::getMerchantId, merchant.getId())
        );
        if (merchantAuth != null) {
            merchantDTO.setAuthStatus(merchantAuth.getAuthStatus());
        }

        User user = userMapper.selectById(merchant.getId());
        if (user != null) {
            merchantDTO.setEmail(user.getEmail());
        }
        merchantDTO.setRoles(authPrincipalRemoteService.getRoleCodesByUserId(merchant.getId()));

        return merchantDTO;
    }

    private List<MerchantDTO> toEnrichedDTOList(List<Merchant> merchants) {
        if (merchants == null || merchants.isEmpty()) {
            return List.of();
        }

        List<Long> merchantIds = merchants.stream()
                .map(Merchant::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Integer> authStatusMap = loadAuthStatusMap(merchantIds);
        Map<Long, String> emailMap = loadEmailMap(merchantIds);
        Map<Long, List<String>> roleMap = loadRoleMap(merchantIds);

        List<MerchantDTO> dtos = merchants.stream()
                .map(merchantConverter::toDTO)
                .collect(Collectors.toList());
        dtos.forEach(dto -> {
            if (dto == null || dto.getId() == null) {
                return;
            }
            dto.setAuthStatus(authStatusMap.get(dto.getId()));
            dto.setEmail(emailMap.get(dto.getId()));
            dto.setRoles(roleMap.getOrDefault(dto.getId(), List.of()));
        });
        return dtos;
    }

    private Map<Long, Integer> loadAuthStatusMap(List<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Map.of();
        }
        List<MerchantAuth> auths = merchantAuthMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MerchantAuth>()
                        .in(MerchantAuth::getMerchantId, merchantIds)
        );
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (MerchantAuth auth : auths) {
            if (auth != null && auth.getMerchantId() != null) {
                result.put(auth.getMerchantId(), auth.getAuthStatus());
            }
        }
        return result;
    }

    private Map<Long, String> loadEmailMap(List<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(merchantIds);
        Map<Long, String> result = new LinkedHashMap<>();
        for (User user : users) {
            if (user != null && user.getId() != null) {
                result.put(user.getId(), user.getEmail());
            }
        }
        return result;
    }

    private Map<Long, List<String>> loadRoleMap(List<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> roleMap = authPrincipalRemoteService.getRoleCodesByUserIds(merchantIds);
        return roleMap == null ? Map.of() : roleMap;
    }

    private Merchant toMerchantEntity(MerchantUpsertRequestDTO requestDTO) {
        Merchant merchant = new Merchant();
        merchant.setUsername(requestDTO.getUsername());
        merchant.setMerchantName(requestDTO.getMerchantName());
        merchant.setPhone(requestDTO.getPhone());
        merchant.setStatus(requestDTO.getStatus());
        return merchant;
    }

    private Merchant resolveCurrentMerchant(MerchantUpsertRequestDTO requestDTO, Merchant existing) {
        Merchant merged = new Merchant();
        merged.setId(existing.getId());
        merged.setUsername(StrUtil.blankToDefault(requestDTO.getUsername(), existing.getUsername()));
        merged.setMerchantName(StrUtil.blankToDefault(requestDTO.getMerchantName(), existing.getMerchantName()));
        merged.setPhone(requestDTO.getPhone() == null ? existing.getPhone() : requestDTO.getPhone());
        merged.setStatus(requestDTO.getStatus() == null ? existing.getStatus() : requestDTO.getStatus());
        return merged;
    }

    private boolean isValidEnableStatus(Integer status) {
        if (status == null) {
            return false;
        }
        return STATUS_ENABLED.equals(status) || STATUS_DISABLED.equals(status);
    }

    private boolean isValidAuditStatus(Integer status) {
        if (status == null) {
            return false;
        }
        return STATUS_PENDING.equals(status) || STATUS_APPROVED.equals(status) || STATUS_REJECTED.equals(status);
    }

    private AuthPrincipalDTO toAuthPrincipalDTO(Merchant merchant, String email, String password) {
        AuthPrincipalDTO authPrincipalDTO = new AuthPrincipalDTO();
        authPrincipalDTO.setId(merchant.getId());
        authPrincipalDTO.setUsername(merchant.getUsername());
        authPrincipalDTO.setPassword(password);
        authPrincipalDTO.setNickname(merchant.getMerchantName());
        authPrincipalDTO.setEmail(email);
        authPrincipalDTO.setPhone(merchant.getPhone());
        authPrincipalDTO.setStatus(merchant.getStatus());
        authPrincipalDTO.setRoles(List.of("USER", "MERCHANT"));
        return authPrincipalDTO;
    }

    private void evictUsernameCacheIfChanged(String oldUsername, String newUsername) {
        if (StrUtil.isBlank(oldUsername) || StrUtil.isBlank(newUsername)) {
            return;
        }
        if (StrUtil.equals(oldUsername, newUsername)) {
            return;
        }
        Cache cache = cacheManager.getCache(MERCHANT_CACHE);
        if (cache != null) {
            cache.evict("username:" + oldUsername);
        }
    }
}


