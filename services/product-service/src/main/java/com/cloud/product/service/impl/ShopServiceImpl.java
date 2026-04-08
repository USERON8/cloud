package com.cloud.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.PageResult;
import com.cloud.product.converter.ShopConverter;
import com.cloud.product.mapper.ShopMapper;
import com.cloud.product.module.dto.ShopPageDTO;
import com.cloud.product.module.dto.ShopRequestDTO;
import com.cloud.product.module.entity.Shop;
import com.cloud.product.module.vo.ShopVO;
import com.cloud.product.service.ShopService;
import com.cloud.product.service.cache.ShopRedisCacheService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

  private final ShopConverter shopConverter;
  private final ShopRedisCacheService shopRedisCacheService;

  @Value("${product.config.batch.max-size:100}")
  private Integer shopListMaxSize;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Long createShop(ShopRequestDTO requestDTO) {
    validateShopRequest(requestDTO, true);

    Shop shop = shopConverter.requestDTOToEntity(requestDTO);
    if (shop.getStatus() == null) {
      shop.setStatus(1);
    }

    boolean saved = save(shop);
    if (!saved) {
      throw new BizException("Create shop failed");
    }
    shopRedisCacheService.clearAllAfterCommit();
    return shop.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean updateShop(Long id, ShopRequestDTO requestDTO) {
    validateId(id);
    validateShopRequest(requestDTO, false);

    Shop existingShop = requireExistingShop(id);
    applyRequest(existingShop, requestDTO);

    boolean updated = updateById(existingShop);
    if (!updated) {
      throw new BizException("Update shop failed");
    }
    shopRedisCacheService.clearAllAfterCommit();
    return true;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean deleteShop(Long id) {
    validateId(id);
    requireExistingShop(id);

    boolean deleted = removeById(id);
    if (!deleted) {
      throw new BizException("Delete shop failed");
    }
    shopRedisCacheService.clearAllAfterCommit();
    return true;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean batchDeleteShops(List<Long> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return true;
    }

    boolean deleted = removeBatchByIds(ids);
    if (!deleted) {
      throw new BizException("Batch delete shops failed");
    }
    shopRedisCacheService.clearAllAfterCommit();
    return true;
  }

  @Override
  @Transactional(readOnly = true)
  public ShopVO getShopById(Long id) {
    validateId(id);
    log.debug("Query shop by id: {}", id);
    ShopVO cached = shopRedisCacheService.getById(id);
    if (cached != null) {
      return cached;
    }
    ShopVO result = shopConverter.toVO(requireExistingShop(id));
    shopRedisCacheService.putById(id, result);
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ShopVO> getShopsByIds(List<Long> ids) {
    log.debug("Batch query shops: ids={}", ids);
    if (CollectionUtils.isEmpty(ids)) {
      return new ArrayList<>();
    }
    return shopConverter.toVOList(listByIds(ids));
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<ShopVO> getShopsPage(ShopPageDTO pageDTO) {
    ShopPageDTO request = pageDTO == null ? new ShopPageDTO() : pageDTO;
    PageResult<ShopVO> cached = shopRedisCacheService.getPage(request);
    if (cached != null) {
      return cached;
    }
    long current =
        request.getCurrent() == null || request.getCurrent() <= 0 ? 1L : request.getCurrent();
    long size = request.getSize() == null || request.getSize() <= 0 ? 20L : request.getSize();
    log.debug(
        "Query shops by page: current={}, size={}, merchantId={}, keyword={}, addressKeyword={}, status={}, createTimeSort={}, updateTimeSort={}",
        current,
        size,
        request.getMerchantId(),
        request.getShopNameKeyword(),
        request.getAddressKeyword(),
        request.getStatus(),
        request.getCreateTimeSort(),
        request.getUpdateTimeSort());

    Page<Shop> page = new Page<>(current, size);
    LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
    if (request.getMerchantId() != null) {
      queryWrapper.eq(Shop::getMerchantId, request.getMerchantId());
    }
    if (StrUtil.isNotBlank(request.getShopNameKeyword())) {
      queryWrapper.like(Shop::getShopName, request.getShopNameKeyword());
    }
    if (StrUtil.isNotBlank(request.getAddressKeyword())) {
      queryWrapper.like(Shop::getAddress, request.getAddressKeyword());
    }
    if (request.getStatus() != null) {
      queryWrapper.eq(Shop::getStatus, request.getStatus());
    }
    applyPageSort(queryWrapper, request);

    Page<Shop> shopPage = page(page, queryWrapper);
    List<ShopVO> shopVOs = shopConverter.toVOList(shopPage.getRecords());
    PageResult<ShopVO> result =
        PageResult.of(shopVOs, shopPage.getTotal(), shopPage.getCurrent(), shopPage.getSize());
    shopRedisCacheService.putPage(request, result);
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ShopVO> getShopsByMerchantId(Long merchantId, Integer status) {
    log.debug("Query shops by merchant: merchantId={}, status={}", merchantId, status);
    List<ShopVO> cached = shopRedisCacheService.getMerchantList(merchantId, status);
    if (cached != null) {
      return cached;
    }
    LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(Shop::getMerchantId, merchantId);
    if (status != null) {
      queryWrapper.eq(Shop::getStatus, status);
    }
    queryWrapper.orderByDesc(Shop::getCreatedAt);
    queryWrapper.last("LIMIT " + resolveShopListLimit());
    List<ShopVO> result = shopConverter.toVOList(list(queryWrapper));
    shopRedisCacheService.putMerchantList(merchantId, status, result);
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ShopVO> searchShopsByName(String shopName, Integer status) {
    log.debug("Search shops by name: shopName={}, status={}", shopName, status);
    if (StrUtil.isBlank(shopName)) {
      return new ArrayList<>();
    }
    List<ShopVO> cached = shopRedisCacheService.getSearchList(shopName, status);
    if (cached != null) {
      return cached;
    }

    LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.like(Shop::getShopName, shopName);
    if (status != null) {
      queryWrapper.eq(Shop::getStatus, status);
    }
    queryWrapper.orderByDesc(Shop::getCreatedAt);
    queryWrapper.last("LIMIT " + resolveShopListLimit());
    List<ShopVO> result = shopConverter.toVOList(list(queryWrapper));
    shopRedisCacheService.putSearchList(shopName, status, result);
    return result;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean enableShop(Long id) {
    return updateShopStatus(id, 1, "Enable");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean disableShop(Long id) {
    return updateShopStatus(id, 0, "Disable");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean batchEnableShops(List<Long> ids) {
    return batchUpdateShopStatus(ids, 1, "Batch enable");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean batchDisableShops(List<Long> ids) {
    return batchUpdateShopStatus(ids, 0, "Batch disable");
  }

  @Override
  @Transactional(readOnly = true)
  public Long getTotalShopCount() {
    Long cached = shopRedisCacheService.getStat("total");
    if (cached != null) {
      return cached;
    }
    Long result = count();
    shopRedisCacheService.putStat("total", result);
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public Long getEnabledShopCount() {
    Long cached = shopRedisCacheService.getStat("enabled");
    if (cached != null) {
      return cached;
    }
    Long result = count(new LambdaQueryWrapper<Shop>().eq(Shop::getStatus, 1));
    shopRedisCacheService.putStat("enabled", result);
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public Long getDisabledShopCount() {
    Long cached = shopRedisCacheService.getStat("disabled");
    if (cached != null) {
      return cached;
    }
    Long result = count(new LambdaQueryWrapper<Shop>().eq(Shop::getStatus, 0));
    shopRedisCacheService.putStat("disabled", result);
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public Long getShopCountByMerchantId(Long merchantId) {
    Long cached = shopRedisCacheService.getStat("merchant:" + merchantId);
    if (cached != null) {
      return cached;
    }
    Long result = count(new LambdaQueryWrapper<Shop>().eq(Shop::getMerchantId, merchantId));
    shopRedisCacheService.putStat("merchant:" + merchantId, result);
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public Boolean hasPermission(Long merchantId, Long shopId) {
    Boolean cached = shopRedisCacheService.getPermission(merchantId, shopId);
    if (cached != null) {
      return cached;
    }
    LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(Shop::getId, shopId).eq(Shop::getMerchantId, merchantId);
    Boolean result = count(queryWrapper) > 0;
    shopRedisCacheService.putPermission(merchantId, shopId, result);
    return result;
  }

  @Override
  public void evictShopCache(Long id) {
    shopRedisCacheService.evictByIdAfterCommit(id);
  }

  @Override
  public void evictAllShopCache() {
    shopRedisCacheService.clearAllAfterCommit();
  }

  @Override
  public void warmupShopCache(List<Long> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return;
    }
    for (Long id : ids) {
      try {
        getShopById(id);
      } catch (Exception e) {
        log.warn("Warmup shop cache failed: id={}", id, e);
      }
    }
  }

  private void validateId(Long id) {
    if (id == null || id <= 0) {
      throw new BizException("Invalid shop id");
    }
  }

  private Shop requireExistingShop(Long id) {
    Shop shop = getById(id);
    if (shop == null) {
      throw new BizException("Shop not found: " + id);
    }
    return shop;
  }

  private void validateShopRequest(ShopRequestDTO requestDTO, boolean create) {
    if (requestDTO == null) {
      throw new BizException("Shop payload cannot be null");
    }
    if (create && requestDTO.getMerchantId() == null) {
      throw new BizException("Merchant id is required");
    }
    if (create && StrUtil.isBlank(requestDTO.getShopName())) {
      throw new BizException("Shop name cannot be blank");
    }
    if (create && StrUtil.isBlank(requestDTO.getContactPhone())) {
      throw new BizException("Contact phone cannot be blank");
    }
    if (create && StrUtil.isBlank(requestDTO.getAddress())) {
      throw new BizException("Address cannot be blank");
    }
    if (requestDTO.getStatus() != null
        && requestDTO.getStatus() != 0
        && requestDTO.getStatus() != 1) {
      throw new BizException("Shop status must be 0 or 1");
    }
  }

  private void applyRequest(Shop shop, ShopRequestDTO requestDTO) {
    if (StrUtil.isNotBlank(requestDTO.getShopName())) {
      shop.setShopName(requestDTO.getShopName());
    }
    if (StrUtil.isNotBlank(requestDTO.getAvatarUrl())) {
      shop.setAvatarUrl(requestDTO.getAvatarUrl());
    }
    if (StrUtil.isNotBlank(requestDTO.getDescription())) {
      shop.setDescription(requestDTO.getDescription());
    }
    if (StrUtil.isNotBlank(requestDTO.getContactPhone())) {
      shop.setContactPhone(requestDTO.getContactPhone());
    }
    if (StrUtil.isNotBlank(requestDTO.getAddress())) {
      shop.setAddress(requestDTO.getAddress());
    }
    if (requestDTO.getStatus() != null) {
      shop.setStatus(requestDTO.getStatus());
    }
  }

  private Boolean updateShopStatus(Long id, Integer status, String operation) {
    requireExistingShop(id);
    LambdaUpdateWrapper<Shop> updateWrapper = new LambdaUpdateWrapper<>();
    updateWrapper.eq(Shop::getId, id).set(Shop::getStatus, status);
    boolean updated = update(updateWrapper);
    if (!updated) {
      throw new BizException(operation + " shop failed");
    }
    shopRedisCacheService.clearAllAfterCommit();
    return true;
  }

  private Boolean batchUpdateShopStatus(List<Long> ids, Integer status, String operation) {
    if (CollectionUtils.isEmpty(ids)) {
      return true;
    }
    LambdaUpdateWrapper<Shop> updateWrapper = new LambdaUpdateWrapper<>();
    updateWrapper.in(Shop::getId, ids).set(Shop::getStatus, status);
    boolean updated = update(updateWrapper);
    if (!updated) {
      throw new BizException(operation + " shops failed");
    }
    shopRedisCacheService.clearAllAfterCommit();
    return true;
  }

  private int resolveShopListLimit() {
    return (shopListMaxSize == null || shopListMaxSize <= 0) ? 100 : shopListMaxSize;
  }

  private void applyPageSort(LambdaQueryWrapper<Shop> queryWrapper, ShopPageDTO request) {
    boolean hasUpdateSort = StrUtil.isNotBlank(request.getUpdateTimeSort());
    boolean hasCreateSort = StrUtil.isNotBlank(request.getCreateTimeSort());
    if (hasUpdateSort) {
      boolean asc = "asc".equalsIgnoreCase(request.getUpdateTimeSort());
      queryWrapper.orderBy(true, asc, Shop::getUpdatedAt);
      queryWrapper.orderBy(true, false, Shop::getCreatedAt);
      return;
    }
    if (hasCreateSort) {
      boolean asc = "asc".equalsIgnoreCase(request.getCreateTimeSort());
      queryWrapper.orderBy(true, asc, Shop::getCreatedAt);
      return;
    }
    queryWrapper.orderByDesc(Shop::getCreatedAt);
  }
}
