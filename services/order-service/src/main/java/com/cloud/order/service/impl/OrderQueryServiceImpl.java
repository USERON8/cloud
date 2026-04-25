package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.api.product.ProductDubboApi;
import com.cloud.api.user.UserDubboApi;
import com.cloud.common.domain.dto.order.ProductSellStatDTO;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.exception.BizException;
import com.cloud.common.remote.RemoteCallSupport;
import com.cloud.common.result.PageResult;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.order.dto.OrderSummaryDTO;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderQueryService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.OrderRefundSagaCoordinator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OrderQueryServiceImpl implements OrderQueryService {

  private static final TypeReference<Map<String, Object>> SNAPSHOT_TYPE = new TypeReference<>() {};

  private final OrderService orderService;
  private final OrderMainMapper orderMainMapper;
  private final OrderSubMapper orderSubMapper;
  private final OrderItemMapper orderItemMapper;
  private final AfterSaleMapper afterSaleMapper;
  private final RemoteCallSupport remoteCallSupport;
  private final ObjectMapper objectMapper;

  @org.apache.dubbo.config.annotation.DubboReference private ProductDubboApi productDubboApi;

  @org.apache.dubbo.config.annotation.DubboReference(check = false, timeout = 5000, retries = 0)
  private UserDubboApi userDubboApi;

  @Override
  public PageResult<OrderSummaryDTO> listOrders(
      Authentication authentication,
      Integer page,
      Integer size,
      Long userId,
      Long merchantId,
      Integer status) {
    int safePage = page == null || page < 1 ? 1 : page;
    int safeSize = size == null || size <= 0 ? 20 : size;
    safeSize = Math.min(safeSize, 100);

    IPage<OrderMain> pageResult =
        queryMainOrders(authentication, safePage, safeSize, userId, merchantId, status);
    List<OrderMain> mains = pageResult == null ? Collections.emptyList() : pageResult.getRecords();
    Long summaryMerchantId = resolveSummaryMerchantId(authentication, merchantId);
    Map<Long, List<OrderSub>> subOrdersByMainId = loadSubOrdersByMainIds(mains, summaryMerchantId);

    List<OrderSummaryDTO> summaries = new ArrayList<>(mains.size());
    for (OrderMain main : mains) {
      List<OrderSub> subs = subOrdersByMainId.getOrDefault(main.getId(), List.of());
      summaries.add(toSummary(main, subs, loadItemsBySubOrders(subs)));
    }

    long total = pageResult == null ? 0L : pageResult.getTotal();
    return PageResult.of((long) safePage, (long) safeSize, total, summaries);
  }

  @Override
  public OrderSummaryDTO getOrderSummary(Long orderId, Authentication authentication) {
    OrderMain main = requireAccessibleMainOrder(orderId, authentication);
    Long summaryMerchantId = resolveSummaryMerchantId(authentication, null);
    List<OrderSub> subs =
        filterSummarySubOrders(orderService.listSubOrders(orderId), summaryMerchantId);
    return toSummary(main, subs, loadItemsBySubOrders(subs));
  }

  @Override
  public OrderMain requireAccessibleMainOrder(Long orderId, Authentication authentication) {
    OrderMain main = orderService.getMainOrder(orderId);
    if (main == null || Integer.valueOf(1).equals(main.getDeleted())) {
      throw new BizException("main order not found");
    }
    if (isAdmin(authentication)) {
      return main;
    }
    Long currentUserId = requireCurrentUserId(authentication);
    if (isMerchant(authentication)) {
      Long currentMerchantId = requireCurrentMerchantId(authentication);
      boolean belongs =
          orderSubMapper.countActiveByMainOrderIdAndMerchantId(main.getId(), currentMerchantId) > 0;
      if (!belongs) {
        throw new BizException("forbidden");
      }
      return main;
    }
    if (!Objects.equals(main.getUserId(), currentUserId)) {
      throw new BizException("forbidden");
    }
    return main;
  }

  @Override
  public void updateCancelReason(Long mainOrderId, String cancelReason) {
    if (mainOrderId == null) {
      return;
    }
    if (cancelReason == null || cancelReason.isBlank()) {
      return;
    }
    OrderMain main = orderMainMapper.selectById(mainOrderId);
    if (main == null || Integer.valueOf(1).equals(main.getDeleted())) {
      throw new BizException("main order not found");
    }
    main.setCancelReason(cancelReason.trim());
    orderMainMapper.updateById(main);
  }

  @Override
  public OrderSubStatusVO getSubOrderStatus(String mainOrderNo, String subOrderNo) {
    if (!StringUtils.hasText(mainOrderNo) || !StringUtils.hasText(subOrderNo)) {
      return null;
    }
    OrderMain mainOrder = orderMainMapper.selectActiveByOrderNo(mainOrderNo.trim());
    if (mainOrder == null) {
      return null;
    }
    OrderSub subOrder =
        orderSubMapper.selectActiveByMainOrderIdAndSubOrderNo(mainOrder.getId(), subOrderNo.trim());
    if (subOrder == null) {
      return null;
    }

    OrderSubStatusVO vo = new OrderSubStatusVO();
    vo.setMainOrderId(mainOrder.getId());
    vo.setSubOrderId(subOrder.getId());
    vo.setMainOrderNo(mainOrder.getMainOrderNo());
    vo.setSubOrderNo(subOrder.getSubOrderNo());
    vo.setOrderStatus(subOrder.getOrderStatus());
    vo.setUserId(mainOrder.getUserId());
    vo.setPayableAmount(subOrder.getPayableAmount());
    return vo;
  }

  @Override
  public List<ProductSellStatDTO> statSellCountToday(Integer limit) {
    int safeLimit = limit == null || limit <= 0 ? 100 : Math.min(limit, 1000);
    LocalDateTime start = LocalDate.now().atStartOfDay();
    LocalDateTime end = start.plusDays(1);
    return orderItemMapper.listDailySellStats(start, end, safeLimit);
  }

  @Override
  public List<ProductSellStatDTO> statSellCountByProductIds(List<Long> productIds) {
    if (productIds == null || productIds.isEmpty()) {
      return List.of();
    }
    List<Long> safeProductIds =
        productIds.stream()
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .toList();
    if (safeProductIds.isEmpty()) {
      return List.of();
    }
    return orderItemMapper.listSellStatsByProductIds(safeProductIds);
  }

  private boolean isAdmin(Authentication authentication) {
    return SecurityPermissionUtils.isAdmin(authentication);
  }

  private boolean isMerchant(Authentication authentication) {
    return SecurityPermissionUtils.isMerchant(authentication);
  }

  private IPage<OrderMain> queryMainOrders(
      Authentication authentication,
      int page,
      int size,
      Long userId,
      Long merchantId,
      Integer status) {
    Page<OrderMain> pageData = new Page<>(page, size);
    if (status != null) {
      if (isAdmin(authentication)) {
        return orderMainMapper.selectPageByVisibleStatus(pageData, merchantId, userId, status);
      }
      if (isMerchant(authentication)) {
        Long currentMerchantId = requireCurrentMerchantId(authentication);
        return orderMainMapper.selectPageByVisibleStatus(pageData, currentMerchantId, null, status);
      }
      Long currentUserId = requireCurrentUserId(authentication);
      return orderMainMapper.selectPageByVisibleStatus(pageData, null, currentUserId, status);
    }

    if (isAdmin(authentication)) {
      if (merchantId != null) {
        return orderMainMapper.selectPageByMerchant(pageData, merchantId, List.of(), userId);
      }
      return orderMainMapper.selectPageActive(pageData, userId);
    }

    if (isMerchant(authentication)) {
      Long currentMerchantId = requireCurrentMerchantId(authentication);
      return orderMainMapper.selectPageByMerchant(pageData, currentMerchantId, List.of(), null);
    }

    Long currentUserId = requireCurrentUserId(authentication);
    return orderMainMapper.selectPageActive(pageData, currentUserId);
  }

  private Map<Long, List<OrderSub>> loadSubOrdersByMainIds(
      List<OrderMain> mains, Long summaryMerchantId) {
    if (mains == null || mains.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Long> mainIds = mains.stream().map(OrderMain::getId).filter(id -> id != null).toList();
    if (mainIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<OrderSub> subs = orderSubMapper.listActiveByMainOrderIds(mainIds);
    subs = filterSummarySubOrders(subs, summaryMerchantId);
    if (subs == null || subs.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, List<OrderSub>> grouped = new java.util.HashMap<>();
    for (OrderSub sub : subs) {
      if (sub == null || sub.getMainOrderId() == null) {
        continue;
      }
      grouped.computeIfAbsent(sub.getMainOrderId(), ignored -> new ArrayList<>()).add(sub);
    }
    return grouped;
  }

  private List<OrderSub> filterSummarySubOrders(List<OrderSub> subs, Long summaryMerchantId) {
    if (subs == null || subs.isEmpty()) {
      return Collections.emptyList();
    }
    if (summaryMerchantId == null) {
      return subs;
    }
    return subs.stream()
        .filter(sub -> sub != null && Objects.equals(sub.getMerchantId(), summaryMerchantId))
        .toList();
  }

  private OrderSummaryDTO toSummary(
      OrderMain main, List<OrderSub> subs, Map<Long, List<OrderItem>> itemsBySubOrderId) {
    OrderSummaryDTO summary = new OrderSummaryDTO();
    summary.setId(main.getId());
    summary.setOrderNo(main.getMainOrderNo());
    summary.setUserId(main.getUserId());
    summary.setTotalAmount(main.getTotalAmount());
    summary.setPayAmount(main.getPayableAmount());
    summary.setCreatedAt(main.getCreatedAt());
    summary.setOrderStatusRaw(main.getOrderStatus());
    summary.setStatus(resolveStatusCode(subs));
    summary.setItems(buildItemSummaries(subs, itemsBySubOrderId));
    List<OrderSummaryDTO.SubOrderSummaryDTO> subSummaries = buildSubOrderSummaries(subs);
    summary.setSubOrders(subSummaries);
    if (subSummaries.size() == 1) {
      applySingleSubOrderSummary(summary, subSummaries.get(0));
    }
    return summary;
  }

  private List<OrderSummaryDTO.SubOrderSummaryDTO> buildSubOrderSummaries(List<OrderSub> subs) {
    if (subs == null || subs.isEmpty()) {
      return List.of();
    }
    List<OrderSummaryDTO.SubOrderSummaryDTO> summaries = new ArrayList<>(subs.size());
    for (OrderSub sub : subs) {
      if (sub == null) {
        continue;
      }
      OrderSummaryDTO.SubOrderSummaryDTO summary = new OrderSummaryDTO.SubOrderSummaryDTO();
      summary.setSubOrderId(sub.getId());
      summary.setSubOrderNo(sub.getSubOrderNo());
      summary.setMerchantId(sub.getMerchantId());
      summary.setPayAmount(sub.getPayableAmount());
      summary.setStatus(resolveSubStatusCode(sub));
      summary.setOrderStatusRaw(sub.getOrderStatus());
      summary.setAfterSaleStatus(sub.getAfterSaleStatus());

      AfterSale latestAfterSale = findLatestAfterSale(sub.getId());
      if (latestAfterSale != null) {
        summary.setAfterSaleId(latestAfterSale.getId());
        summary.setAfterSaleNo(latestAfterSale.getAfterSaleNo());
        summary.setAfterSaleType(latestAfterSale.getAfterSaleType());
        if (StringUtils.hasText(latestAfterSale.getAfterSaleNo())) {
          summary.setRefundNo(
              OrderRefundSagaCoordinator.buildRefundNo(latestAfterSale.getAfterSaleNo()));
        }
      }
      summaries.add(summary);
    }
    return summaries;
  }

  private void applySingleSubOrderSummary(
      OrderSummaryDTO summary, OrderSummaryDTO.SubOrderSummaryDTO subSummary) {
    if (summary == null || subSummary == null) {
      return;
    }
    summary.setSubOrderId(subSummary.getSubOrderId());
    summary.setSubOrderNo(subSummary.getSubOrderNo());
    summary.setMerchantId(subSummary.getMerchantId());
    summary.setAfterSaleId(subSummary.getAfterSaleId());
    summary.setAfterSaleNo(subSummary.getAfterSaleNo());
    summary.setAfterSaleType(subSummary.getAfterSaleType());
    summary.setRefundNo(subSummary.getRefundNo());
    summary.setOrderStatusRaw(subSummary.getOrderStatusRaw());
    summary.setAfterSaleStatus(subSummary.getAfterSaleStatus());
  }

  private Map<Long, List<OrderItem>> loadItemsBySubOrders(List<OrderSub> subs) {
    if (subs == null || subs.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Long> subOrderIds = subs.stream().map(OrderSub::getId).filter(Objects::nonNull).toList();
    if (subOrderIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<OrderItem> items = orderItemMapper.listActiveBySubOrderIds(subOrderIds);
    if (items == null || items.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, List<OrderItem>> grouped = new HashMap<>();
    for (OrderItem item : items) {
      if (item == null || item.getSubOrderId() == null) {
        continue;
      }
      grouped.computeIfAbsent(item.getSubOrderId(), ignored -> new ArrayList<>()).add(item);
    }
    return grouped;
  }

  private List<OrderSummaryDTO.OrderItemSummaryDTO> buildItemSummaries(
      List<OrderSub> subs, Map<Long, List<OrderItem>> itemsBySubOrderId) {
    if (subs == null
        || subs.isEmpty()
        || itemsBySubOrderId == null
        || itemsBySubOrderId.isEmpty()) {
      return List.of();
    }
    List<OrderItem> allItems = new ArrayList<>();
    for (OrderSub sub : subs) {
      if (sub == null || sub.getId() == null) {
        continue;
      }
      allItems.addAll(itemsBySubOrderId.getOrDefault(sub.getId(), List.of()));
    }
    if (allItems.isEmpty()) {
      return List.of();
    }

    Map<Long, SkuDetailVO> latestSkuMap = loadLatestSkuMap(allItems);
    Map<Long, SpuDetailVO> latestSpuMap = loadLatestSpuMap(latestSkuMap.values());
    List<OrderSummaryDTO.OrderItemSummaryDTO> result = new ArrayList<>(allItems.size());
    for (OrderItem item : allItems) {
      result.add(toItemSummary(item, latestSkuMap.get(item.getSkuId()), latestSpuMap));
    }
    return result;
  }

  private Map<Long, SkuDetailVO> loadLatestSkuMap(List<OrderItem> items) {
    List<Long> skuIds =
        items.stream()
            .map(OrderItem::getSkuId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .toList();
    if (skuIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<SkuDetailVO> skuDetails =
        remoteCallSupport.queryOrFallback(
            "product-service.list sku by ids",
            () -> productDubboApi.listSkuByIds(skuIds),
            ex -> List.of());
    if (skuDetails == null || skuDetails.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, SkuDetailVO> latestSkuMap = new HashMap<>();
    for (SkuDetailVO skuDetail : skuDetails) {
      if (skuDetail != null && skuDetail.getSkuId() != null) {
        latestSkuMap.put(skuDetail.getSkuId(), skuDetail);
      }
    }
    return latestSkuMap;
  }

  private Map<Long, SpuDetailVO> loadLatestSpuMap(java.util.Collection<SkuDetailVO> skuDetails) {
    if (skuDetails == null || skuDetails.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Long> spuIds =
        skuDetails.stream()
            .map(SkuDetailVO::getSpuId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .toList();
    if (spuIds.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, SpuDetailVO> latestSpuMap = new HashMap<>();
    for (Long spuId : spuIds) {
      SpuDetailVO spuDetail =
          remoteCallSupport.queryOrFallback(
              "product-service.get spu by id", () -> productDubboApi.getSpuById(spuId), ex -> null);
      if (spuDetail != null && spuDetail.getSpuId() != null) {
        latestSpuMap.put(spuDetail.getSpuId(), spuDetail);
      }
    }
    return latestSpuMap;
  }

  private OrderSummaryDTO.OrderItemSummaryDTO toItemSummary(
      OrderItem item, SkuDetailVO latestSku, Map<Long, SpuDetailVO> latestSpuMap) {
    OrderSummaryDTO.OrderItemSummaryDTO summary = new OrderSummaryDTO.OrderItemSummaryDTO();
    summary.setId(item.getId());
    summary.setSubOrderId(item.getSubOrderId());
    summary.setSpuId(item.getSpuId());
    summary.setSkuId(item.getSkuId());
    summary.setSkuCode(item.getSkuCode());
    summary.setSkuName(item.getSkuName());
    summary.setQuantity(item.getQuantity());
    summary.setUnitPrice(item.getUnitPrice());
    summary.setTotalPrice(item.getTotalPrice());
    summary.setSkuSnapshot(parseSnapshot(item.getSkuSnapshot()));
    summary.setLatestProduct(buildLatestProduct(item, latestSku, latestSpuMap));
    return summary;
  }

  private Map<String, Object> parseSnapshot(String snapshot) {
    if (!StringUtils.hasText(snapshot)) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.readValue(snapshot, SNAPSHOT_TYPE);
    } catch (Exception ex) {
      Map<String, Object> fallback = new HashMap<>();
      fallback.put("raw", snapshot);
      return fallback;
    }
  }

  private OrderSummaryDTO.LatestProductDTO buildLatestProduct(
      OrderItem item, SkuDetailVO latestSku, Map<Long, SpuDetailVO> latestSpuMap) {
    if (latestSku == null) {
      return null;
    }
    OrderSummaryDTO.LatestProductDTO latestProduct = new OrderSummaryDTO.LatestProductDTO();
    latestProduct.setSkuId(latestSku.getSkuId());
    latestProduct.setSpuId(latestSku.getSpuId());
    latestProduct.setSkuCode(latestSku.getSkuCode());
    latestProduct.setSkuName(latestSku.getSkuName());
    latestProduct.setSpecJson(latestSku.getSpecJson());
    latestProduct.setSalePrice(latestSku.getSalePrice());
    latestProduct.setMarketPrice(latestSku.getMarketPrice());
    latestProduct.setImageUrl(latestSku.getImageUrl());
    latestProduct.setImageFile(latestSku.getImageFile());
    latestProduct.setStatus(latestSku.getStatus());
    SpuDetailVO latestSpu = latestSpuMap.getOrDefault(latestSku.getSpuId(), null);
    if (latestSpu != null) {
      latestProduct.setSpuName(latestSpu.getSpuName());
      latestProduct.setBrandName(latestSpu.getBrandName());
      latestProduct.setCategoryName(latestSpu.getCategoryName());
      latestProduct.setMerchantId(latestSpu.getMerchantId());
      latestProduct.setShopName(latestSpu.getShopName());
    } else {
      latestProduct.setSpuId(item.getSpuId());
    }
    return latestProduct;
  }

  private AfterSale findLatestAfterSale(Long subOrderId) {
    if (subOrderId == null) {
      return null;
    }
    return afterSaleMapper.selectLatestActiveBySubOrderId(subOrderId);
  }

  private Integer resolveStatusCode(List<OrderSub> subs) {
    if (subs == null || subs.isEmpty()) {
      return 0;
    }
    boolean allDone = subs.stream().allMatch(sub -> "DONE".equals(sub.getOrderStatus()));
    if (allDone) {
      return 3;
    }
    boolean allClosed =
        subs.stream()
            .allMatch(
                sub ->
                    "CANCELLED".equals(sub.getOrderStatus())
                        || "CLOSED".equals(sub.getOrderStatus()));
    if (allClosed) {
      return 4;
    }
    boolean anyShipped = subs.stream().anyMatch(sub -> "SHIPPED".equals(sub.getOrderStatus()));
    if (anyShipped) {
      return 2;
    }
    boolean anyPaid = subs.stream().anyMatch(sub -> "PAID".equals(sub.getOrderStatus()));
    if (anyPaid) {
      return 1;
    }
    return 0;
  }

  private Integer resolveSubStatusCode(OrderSub sub) {
    if (sub == null) {
      return 0;
    }
    if ("DONE".equals(sub.getOrderStatus())) {
      return 3;
    }
    if ("CANCELLED".equals(sub.getOrderStatus()) || "CLOSED".equals(sub.getOrderStatus())) {
      return 4;
    }
    if ("SHIPPED".equals(sub.getOrderStatus())) {
      return 2;
    }
    if ("PAID".equals(sub.getOrderStatus())) {
      return 1;
    }
    return 0;
  }

  private Long resolveSummaryMerchantId(Authentication authentication, Long requestedMerchantId) {
    if (isMerchant(authentication)) {
      return requireCurrentMerchantId(authentication);
    }
    return requestedMerchantId;
  }

  private Long requireCurrentMerchantId(Authentication authentication) {
    Long currentUserId = requireCurrentUserId(authentication);
    Long currentMerchantId = userDubboApi.findMerchantIdByOwnerUserId(currentUserId);
    if (currentMerchantId == null) {
      throw new BizException("current merchant not found");
    }
    return currentMerchantId;
  }

  private Long requireCurrentUserId(Authentication authentication) {
    return SecurityPermissionUtils.requireCurrentUserIdAsLong(authentication);
  }
}
