package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.dto.order.ProductSellStatDTO;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.PageResult;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.order.dto.OrderSummaryDTO;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderQueryService;
import com.cloud.order.service.OrderService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

  private final OrderService orderService;
  private final OrderMainMapper orderMainMapper;
  private final OrderSubMapper orderSubMapper;
  private final OrderItemMapper orderItemMapper;

  @Override
  public PageResult<OrderSummaryDTO> listOrders(
      Authentication authentication,
      Integer page,
      Integer size,
      Long userId,
      Long shopId,
      Integer status) {
    int safePage = page == null || page < 1 ? 1 : page;
    int safeSize = size == null || size <= 0 ? 20 : size;
    safeSize = Math.min(safeSize, 100);

    List<String> statusFilters = resolveMainStatusFilters(status);
    IPage<OrderMain> pageResult =
        queryMainOrders(authentication, safePage, safeSize, userId, shopId, statusFilters);
    List<OrderMain> mains = pageResult == null ? Collections.emptyList() : pageResult.getRecords();
    Map<Long, List<OrderSub>> subOrdersByMainId = loadSubOrdersByMainIds(mains);

    List<OrderSummaryDTO> summaries = new ArrayList<>(mains.size());
    for (OrderMain main : mains) {
      List<OrderSub> subs = subOrdersByMainId.getOrDefault(main.getId(), List.of());
      OrderSummaryDTO summary = toSummary(main, subs);
      if (status != null && !status.equals(summary.getStatus())) {
        continue;
      }
      summaries.add(summary);
    }

    long total = pageResult == null ? 0L : pageResult.getTotal();
    return PageResult.of((long) safePage, (long) safeSize, total, summaries);
  }

  @Override
  public OrderSummaryDTO getOrderSummary(Long orderId, Authentication authentication) {
    OrderMain main = requireAccessibleMainOrder(orderId, authentication);
    List<OrderSub> subs = orderService.listSubOrders(orderId);
    return toSummary(main, subs);
  }

  @Override
  public OrderMain requireAccessibleMainOrder(Long orderId, Authentication authentication) {
    OrderMain main = orderService.getMainOrder(orderId);
    if (main == null || Integer.valueOf(1).equals(main.getDeleted())) {
      throw new BusinessException("main order not found");
    }
    if (isAdmin(authentication)) {
      return main;
    }
    Long currentUserId = requireCurrentUserId(authentication);
    if (isMerchant(authentication)) {
      boolean belongs =
          orderSubMapper.selectCount(
                  new LambdaQueryWrapper<OrderSub>()
                      .eq(OrderSub::getMainOrderId, main.getId())
                      .eq(OrderSub::getMerchantId, currentUserId)
                      .eq(OrderSub::getDeleted, 0))
              > 0;
      if (!belongs) {
        throw new BusinessException("forbidden");
      }
      return main;
    }
    if (!Objects.equals(main.getUserId(), currentUserId)) {
      throw new BusinessException("forbidden");
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
      throw new BusinessException("main order not found");
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
    return vo;
  }

  @Override
  public List<ProductSellStatDTO> statSellCountToday(Integer limit) {
    int safeLimit = limit == null || limit <= 0 ? 100 : Math.min(limit, 1000);
    LocalDateTime start = LocalDate.now().atStartOfDay();
    LocalDateTime end = start.plusDays(1);
    return orderItemMapper.listDailySellStats(start, end, safeLimit);
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
      Long shopId,
      List<String> statusFilters) {
    Page<OrderMain> pageData = new Page<>(page, size);
    if (isAdmin(authentication)) {
      if (shopId != null) {
        return orderMainMapper.selectPageByMerchant(pageData, shopId, statusFilters, userId);
      }
      LambdaQueryWrapper<OrderMain> wrapper =
          new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getDeleted, 0);
      if (userId != null) {
        wrapper.eq(OrderMain::getUserId, userId);
      }
      if (statusFilters != null && !statusFilters.isEmpty()) {
        wrapper.in(OrderMain::getOrderStatus, statusFilters);
      }
      wrapper.orderByDesc(OrderMain::getId);
      return orderMainMapper.selectPage(pageData, wrapper);
    }

    if (isMerchant(authentication)) {
      Long merchantId = requireCurrentUserId(authentication);
      return orderMainMapper.selectPageByMerchant(pageData, merchantId, statusFilters, null);
    }

    Long currentUserId = requireCurrentUserId(authentication);
    LambdaQueryWrapper<OrderMain> wrapper =
        new LambdaQueryWrapper<OrderMain>()
            .eq(OrderMain::getDeleted, 0)
            .eq(OrderMain::getUserId, currentUserId);
    if (statusFilters != null && !statusFilters.isEmpty()) {
      wrapper.in(OrderMain::getOrderStatus, statusFilters);
    }
    wrapper.orderByDesc(OrderMain::getId);
    return orderMainMapper.selectPage(pageData, wrapper);
  }

  private Map<Long, List<OrderSub>> loadSubOrdersByMainIds(List<OrderMain> mains) {
    if (mains == null || mains.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Long> mainIds = mains.stream().map(OrderMain::getId).filter(id -> id != null).toList();
    if (mainIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<OrderSub> subs =
        orderSubMapper.selectList(
            new LambdaQueryWrapper<OrderSub>()
                .in(OrderSub::getMainOrderId, mainIds)
                .eq(OrderSub::getDeleted, 0));
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

  private OrderSummaryDTO toSummary(OrderMain main, List<OrderSub> subs) {
    OrderSummaryDTO summary = new OrderSummaryDTO();
    summary.setId(main.getId());
    summary.setOrderNo(main.getMainOrderNo());
    summary.setUserId(main.getUserId());
    summary.setTotalAmount(main.getTotalAmount());
    summary.setPayAmount(main.getPayableAmount());
    summary.setCreatedAt(main.getCreatedAt());
    summary.setStatus(resolveStatusCode(subs));
    return summary;
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

  private List<String> resolveMainStatusFilters(Integer statusCode) {
    if (statusCode == null) {
      return List.of();
    }
    return switch (statusCode) {
      case 0 -> List.of("CREATED", "STOCK_RESERVED");
      case 1 -> List.of("PAID");
      case 2 -> List.of("PAID", "SHIPPED");
      case 3 -> List.of("DONE");
      case 4 -> List.of("CANCELLED", "CLOSED");
      default -> List.of();
    };
  }

  private Long requireCurrentUserId(Authentication authentication) {
    String userId = SecurityPermissionUtils.getCurrentUserId(authentication);
    if (userId == null || userId.isBlank()) {
      throw new BusinessException("current user not found in token");
    }
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException ex) {
      throw new BusinessException("invalid user_id in token");
    }
  }
}
