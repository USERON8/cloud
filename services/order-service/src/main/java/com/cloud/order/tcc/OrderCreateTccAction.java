package com.cloud.order.tcc;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.RemoteException;
import com.cloud.common.messaging.event.OrderTimeoutEvent;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.entity.Cart;
import com.cloud.order.entity.CartItem;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.entity.OrderTccLog;
import com.cloud.order.mapper.CartItemMapper;
import com.cloud.order.mapper.CartMapper;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.mapper.OrderTccLogMapper;
import com.cloud.order.messaging.OrderTimeoutMessageProducer;
import com.cloud.order.service.support.OrderAggregateCacheService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextParameter;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@LocalTCC
@Component
@RequiredArgsConstructor
public class OrderCreateTccAction {

  private static final String STATUS_TRY = "TRY";
  private static final String STATUS_CONFIRM = "CONFIRM";
  private static final String STATUS_CANCEL = "CANCEL";
  private static final String CART_STATUS_ACTIVE = "ACTIVE";
  private static final String CART_STATUS_CHECKED_OUT = "CHECKED_OUT";

  private static final Set<String> CANCELLABLE_STATUSES = Set.of("CREATED", "STOCK_RESERVED");

  private final OrderMainMapper orderMainMapper;
  private final OrderSubMapper orderSubMapper;
  private final OrderItemMapper orderItemMapper;
  private final OrderTccLogMapper orderTccLogMapper;
  private final OrderAggregateCacheService orderAggregateCacheService;
  private final OrderTimeoutMessageProducer orderTimeoutMessageProducer;
  private final CartMapper cartMapper;
  private final CartItemMapper cartItemMapper;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private ProductDubboApi productDubboApi;

  @TwoPhaseBusinessAction(
      name = "orderCreateTcc",
      commitMethod = "commit",
      rollbackMethod = "rollback")
  @Transactional(rollbackFor = Exception.class)
  public boolean prepare(
      BusinessActionContext actionContext,
      @BusinessActionContextParameter(paramName = "idempotencyKey") String idempotencyKey,
      @BusinessActionContextParameter(paramName = "cartId") Long cartId,
      CreateMainOrderRequest request) {
    if (StrUtil.isBlank(idempotencyKey)) {
      throw new BizException("idempotency key is required");
    }
    if (request == null) {
      throw new BizException("order request is required");
    }
    if (request.getCartId() != null) {
      buildFromCart(request);
    } else {
      buildFromSingleItem(request);
    }
    if (request.getSubOrders() == null || request.getSubOrders().isEmpty()) {
      throw new BizException("subOrders is required");
    }

    OrderTccLog existingLog = findLog(idempotencyKey);
    if (existingLog != null) {
      if (STATUS_CANCEL.equals(existingLog.getStatus())) {
        throw new BizException("order create tcc cancelled");
      }
      return true;
    }

    OrderTccLog logEntity = new OrderTccLog();
    logEntity.setBusinessKey(idempotencyKey);
    logEntity.setStatus(STATUS_TRY);
    orderTccLogMapper.insert(logEntity);

    OrderMain existing = orderMainMapper.selectActiveByIdempotencyKey(idempotencyKey);
    if (existing != null) {
      attachOrderInfo(logEntity, existing);
      return true;
    }

    OrderMain mainOrder = buildMainOrder(request, idempotencyKey);
    try {
      orderMainMapper.insert(mainOrder);
    } catch (DuplicateKeyException duplicateKeyException) {
      OrderMain duplicated = orderMainMapper.selectActiveByIdempotencyKey(idempotencyKey);
      if (duplicated != null) {
        attachOrderInfo(logEntity, duplicated);
        return true;
      }
      throw duplicateKeyException;
    }

    for (CreateMainOrderRequest.CreateSubOrderRequest subRequest : request.getSubOrders()) {
      OrderSub subOrder = buildSubOrder(mainOrder, subRequest);
      orderSubMapper.insert(subOrder);

      for (CreateMainOrderRequest.CreateOrderItemRequest itemRequest : subRequest.getItems()) {
        OrderItem item = buildOrderItem(mainOrder, subOrder, itemRequest);
        orderItemMapper.insert(item);
      }
    }

    attachOrderInfo(logEntity, mainOrder);
    if (request.getCartId() != null) {
      markCartCheckedOut(request.getCartId(), request.getUserId());
    }
    return true;
  }

  @Transactional(rollbackFor = Exception.class)
  public boolean commit(BusinessActionContext actionContext) {
    String idempotencyKey = resolveBusinessKey(actionContext);
    if (StrUtil.isBlank(idempotencyKey)) {
      return true;
    }
    OrderTccLog logEntity = findLog(idempotencyKey);
    if (logEntity == null) {
      return true;
    }
    if (STATUS_CONFIRM.equals(logEntity.getStatus())
        || STATUS_CANCEL.equals(logEntity.getStatus())) {
      return true;
    }

    logEntity.setStatus(STATUS_CONFIRM);
    orderTccLogMapper.updateById(logEntity);

    OrderMain mainOrder = orderMainMapper.selectActiveByIdempotencyKey(idempotencyKey);
    if (mainOrder == null) {
      return true;
    }
    List<OrderSub> subOrders = orderSubMapper.listActiveByMainOrderId(mainOrder.getId());
    if (subOrders.isEmpty()) {
      return true;
    }
    for (OrderSub subOrder : subOrders) {
      if ("CREATED".equals(subOrder.getOrderStatus())) {
        subOrder.setOrderStatus("STOCK_RESERVED");
        orderSubMapper.updateById(subOrder);
      }
    }
    mainOrder.setOrderStatus("STOCK_RESERVED");
    orderMainMapper.updateById(mainOrder);
    orderAggregateCacheService.evict(mainOrder.getId());

    for (OrderSub subOrder : subOrders) {
      if (!"STOCK_RESERVED".equals(subOrder.getOrderStatus())) {
        continue;
      }
      OrderTimeoutEvent event =
          OrderTimeoutEvent.builder()
              .subOrderId(subOrder.getId())
              .subOrderNo(subOrder.getSubOrderNo())
              .mainOrderNo(mainOrder.getMainOrderNo())
              .userId(mainOrder.getUserId())
              .build();
      orderTimeoutMessageProducer.sendAfterCommit(event);
    }
    return true;
  }

  @Transactional(rollbackFor = Exception.class)
  public boolean rollback(BusinessActionContext actionContext) {
    String idempotencyKey = resolveBusinessKey(actionContext);
    if (StrUtil.isBlank(idempotencyKey)) {
      return true;
    }
    try {
      OrderTccLog logEntity = findLog(idempotencyKey);
      if (logEntity == null) {
        OrderTccLog cancelLog = new OrderTccLog();
        cancelLog.setBusinessKey(idempotencyKey);
        cancelLog.setStatus(STATUS_CANCEL);
        orderTccLogMapper.insert(cancelLog);
        return true;
      }
      if (STATUS_CANCEL.equals(logEntity.getStatus())) {
        return true;
      }
      if (STATUS_CONFIRM.equals(logEntity.getStatus())) {
        return true;
      }

      logEntity.setStatus(STATUS_CANCEL);
      orderTccLogMapper.updateById(logEntity);

      OrderMain mainOrder = orderMainMapper.selectActiveByIdempotencyKey(idempotencyKey);
      if (mainOrder == null) {
        return true;
      }
      List<OrderSub> subOrders = orderSubMapper.listActiveByMainOrderId(mainOrder.getId());
      for (OrderSub subOrder : subOrders) {
        if (!CANCELLABLE_STATUSES.contains(subOrder.getOrderStatus())) {
          continue;
        }
        subOrder.setOrderStatus("CANCELLED");
        subOrder.setClosedAt(LocalDateTime.now());
        subOrder.setCloseReason("tcc rollback");
        orderSubMapper.updateById(subOrder);
      }
      mainOrder.setOrderStatus("CANCELLED");
      mainOrder.setCancelledAt(LocalDateTime.now());
      mainOrder.setCancelReason("tcc rollback");
      orderMainMapper.updateById(mainOrder);
      orderAggregateCacheService.evict(mainOrder.getId());
      Long cartId = resolveCartId(actionContext);
      if (cartId != null) {
        resetCartCheckedOut(cartId);
      }
      return true;
    } catch (Exception ex) {
      log.error("TCC rollback failed: idempotencyKey={}", idempotencyKey, ex);
      return true;
    }
  }

  private OrderTccLog findLog(String idempotencyKey) {
    return orderTccLogMapper.selectOne(
        new LambdaQueryWrapper<OrderTccLog>()
            .eq(OrderTccLog::getBusinessKey, idempotencyKey)
            .eq(OrderTccLog::getDeleted, 0)
            .last("LIMIT 1"));
  }

  private String resolveBusinessKey(BusinessActionContext actionContext) {
    if (actionContext == null) {
      return null;
    }
    Object value = actionContext.getActionContext("idempotencyKey");
    return value == null ? null : String.valueOf(value);
  }

  private void attachOrderInfo(OrderTccLog logEntity, OrderMain mainOrder) {
    if (logEntity == null || mainOrder == null) {
      return;
    }
    logEntity.setMainOrderId(mainOrder.getId());
    logEntity.setMainOrderNo(mainOrder.getMainOrderNo());
    orderTccLogMapper.updateById(logEntity);
  }

  private OrderMain buildMainOrder(CreateMainOrderRequest request, String idempotencyKey) {
    OrderMain mainOrder = new OrderMain();
    mainOrder.setMainOrderNo("M" + UUID.randomUUID().toString().replace("-", ""));
    mainOrder.setUserId(request.getUserId());
    mainOrder.setOrderStatus("CREATED");
    mainOrder.setTotalAmount(defaultAmount(request.getTotalAmount()));
    mainOrder.setPayableAmount(defaultAmount(request.getPayableAmount()));
    mainOrder.setRemark(request.getRemark());
    mainOrder.setIdempotencyKey(idempotencyKey);
    return mainOrder;
  }

  private void buildFromCart(CreateMainOrderRequest request) {
    Long cartId = request.getCartId();
    if (cartId == null) {
      return;
    }
    if (request.getUserId() == null) {
      throw new BizException("user id is required for cart checkout");
    }
    Cart cart = cartMapper.selectById(cartId);
    if (cart == null || cart.getDeleted() == 1) {
      throw new BizException("cart not found");
    }
    if (!request.getUserId().equals(cart.getUserId())) {
      throw new BizException("cart does not belong to current user");
    }
    if (cart.getCartStatus() != null && !CART_STATUS_ACTIVE.equals(cart.getCartStatus())) {
      throw new BizException("cart is not active");
    }

    List<CartItem> cartItems =
        cartItemMapper.selectList(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getCartId, cartId)
                .eq(CartItem::getUserId, request.getUserId())
                .eq(CartItem::getSelected, 1)
                .eq(CartItem::getCheckedOut, 0)
                .eq(CartItem::getDeleted, 0));
    if (cartItems == null || cartItems.isEmpty()) {
      throw new BizException("cart has no selectable items");
    }

    Map<Long, SpuDetailVO> spuDetails = loadSpuDetails(cartItems);
    Map<Long, SkuDetailVO> skuDetails = loadSkuDetails(cartItems);
    Map<Long, List<CartItem>> itemsByMerchant = new HashMap<>();
    for (CartItem item : cartItems) {
      if (item.getSpuId() == null) {
        throw new BizException("cart item missing spu id");
      }
      SpuDetailVO spu = spuDetails.get(item.getSpuId());
      if (spu == null || spu.getMerchantId() == null) {
        throw new BizException("missing merchant for spuId=" + item.getSpuId());
      }
      itemsByMerchant.computeIfAbsent(spu.getMerchantId(), ignored -> new ArrayList<>()).add(item);
    }

    List<CreateMainOrderRequest.CreateSubOrderRequest> subOrders =
        new ArrayList<>(itemsByMerchant.size());
    BigDecimal total = BigDecimal.ZERO;
    for (Map.Entry<Long, List<CartItem>> entry : itemsByMerchant.entrySet()) {
      CreateMainOrderRequest.CreateSubOrderRequest sub =
          new CreateMainOrderRequest.CreateSubOrderRequest();
      sub.setMerchantId(entry.getKey());
      sub.setReceiverName(request.getReceiverName());
      sub.setReceiverPhone(request.getReceiverPhone());
      sub.setReceiverAddress(request.getReceiverAddress());

      BigDecimal itemAmount = BigDecimal.ZERO;
      List<CreateMainOrderRequest.CreateOrderItemRequest> items = new ArrayList<>();
      for (CartItem cartItem : entry.getValue()) {
        BigDecimal unitPrice = cartItem.getUnitPrice();
        if (unitPrice == null) {
          SkuDetailVO skuDetail = skuDetails.get(cartItem.getSkuId());
          unitPrice = skuDetail != null ? skuDetail.getSalePrice() : null;
        }
        if (unitPrice == null) {
          throw new BizException("cart item missing unit price for skuId=" + cartItem.getSkuId());
        }
        Integer quantity = cartItem.getQuantity();
        if (quantity == null || quantity <= 0) {
          throw new BizException("invalid cart item quantity for skuId=" + cartItem.getSkuId());
        }
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        itemAmount = itemAmount.add(totalPrice);

        CreateMainOrderRequest.CreateOrderItemRequest item =
            new CreateMainOrderRequest.CreateOrderItemRequest();
        item.setSpuId(cartItem.getSpuId());
        item.setSkuId(cartItem.getSkuId());
        item.setSkuName(cartItem.getSkuName());
        SkuDetailVO skuDetail = skuDetails.get(cartItem.getSkuId());
        if (skuDetail != null) {
          item.setSkuCode(skuDetail.getSkuCode());
          if (item.getSkuName() == null) {
            item.setSkuName(skuDetail.getSkuName());
          }
        }
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setTotalPrice(totalPrice);
        items.add(item);
      }
      sub.setItems(items);
      sub.setItemAmount(itemAmount);
      sub.setDiscountAmount(BigDecimal.ZERO);
      sub.setShippingFee(BigDecimal.ZERO);
      sub.setPayableAmount(itemAmount);
      total = total.add(itemAmount);
      subOrders.add(sub);
    }

    subOrders.sort(
        Comparator.comparing(CreateMainOrderRequest.CreateSubOrderRequest::getMerchantId));
    request.setSubOrders(subOrders);
    request.setTotalAmount(total);
    request.setPayableAmount(total);
  }

  private void buildFromSingleItem(CreateMainOrderRequest request) {
    if (request.getSpuId() == null || request.getSkuId() == null) {
      return;
    }
    Integer quantity = request.getQuantity();
    if (quantity == null || quantity <= 0) {
      throw new BizException("quantity is required for direct item checkout");
    }

    SpuDetailVO spuDetail =
        invokeProductService("get spu by id", () -> productDubboApi.getSpuById(request.getSpuId()));
    if (spuDetail == null || spuDetail.getMerchantId() == null) {
      throw new BizException("spu not found for direct checkout");
    }
    List<SkuDetailVO> skuDetails =
        invokeProductService(
            "list sku by ids", () -> productDubboApi.listSkuByIds(List.of(request.getSkuId())));
    SkuDetailVO skuDetail = skuDetails == null || skuDetails.isEmpty() ? null : skuDetails.get(0);
    if (skuDetail == null) {
      throw new BizException("sku not found for direct checkout");
    }
    if (skuDetail.getSpuId() != null && !skuDetail.getSpuId().equals(request.getSpuId())) {
      throw new BizException("sku does not belong to requested spu");
    }
    BigDecimal unitPrice = skuDetail.getSalePrice();
    if (unitPrice == null) {
      throw new BizException("sku price missing for direct checkout");
    }
    BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

    CreateMainOrderRequest.CreateOrderItemRequest item =
        new CreateMainOrderRequest.CreateOrderItemRequest();
    item.setSpuId(request.getSpuId());
    item.setSkuId(request.getSkuId());
    item.setSkuCode(skuDetail.getSkuCode());
    item.setSkuName(skuDetail.getSkuName());
    item.setQuantity(quantity);
    item.setUnitPrice(unitPrice);
    item.setTotalPrice(totalPrice);

    CreateMainOrderRequest.CreateSubOrderRequest sub =
        new CreateMainOrderRequest.CreateSubOrderRequest();
    sub.setMerchantId(spuDetail.getMerchantId());
    sub.setReceiverName(request.getReceiverName());
    sub.setReceiverPhone(request.getReceiverPhone());
    sub.setReceiverAddress(request.getReceiverAddress());
    sub.setItems(List.of(item));
    sub.setItemAmount(totalPrice);
    sub.setDiscountAmount(BigDecimal.ZERO);
    sub.setShippingFee(BigDecimal.ZERO);
    sub.setPayableAmount(totalPrice);

    request.setSubOrders(List.of(sub));
    request.setTotalAmount(totalPrice);
    request.setPayableAmount(totalPrice);
  }

  private Map<Long, SpuDetailVO> loadSpuDetails(List<CartItem> cartItems) {
    Map<Long, SpuDetailVO> result = new HashMap<>();
    for (CartItem item : cartItems) {
      if (item.getSpuId() == null || result.containsKey(item.getSpuId())) {
        continue;
      }
      SpuDetailVO detail =
          invokeProductService("get spu by id", () -> productDubboApi.getSpuById(item.getSpuId()));
      if (detail != null) {
        result.put(item.getSpuId(), detail);
      }
    }
    return result;
  }

  private Map<Long, SkuDetailVO> loadSkuDetails(List<CartItem> cartItems) {
    List<Long> skuIds =
        cartItems.stream().map(CartItem::getSkuId).filter(id -> id != null).distinct().toList();
    if (skuIds.isEmpty()) {
      return Map.of();
    }
    List<SkuDetailVO> skuDetails =
        invokeProductService("list sku by ids", () -> productDubboApi.listSkuByIds(skuIds));
    if (skuDetails == null || skuDetails.isEmpty()) {
      return Map.of();
    }
    Map<Long, SkuDetailVO> result = new HashMap<>();
    for (SkuDetailVO detail : skuDetails) {
      if (detail != null && detail.getSkuId() != null) {
        result.put(detail.getSkuId(), detail);
      }
    }
    return result;
  }

  private void markCartCheckedOut(Long cartId, Long userId) {
    if (cartId == null) {
      return;
    }
    cartItemMapper.update(
        null,
        new LambdaUpdateWrapper<CartItem>()
            .eq(CartItem::getCartId, cartId)
            .eq(CartItem::getUserId, userId)
            .eq(CartItem::getCheckedOut, 0)
            .set(CartItem::getCheckedOut, 1));
    cartMapper.update(
        null,
        new LambdaUpdateWrapper<Cart>()
            .eq(Cart::getId, cartId)
            .set(Cart::getCartStatus, CART_STATUS_CHECKED_OUT));
  }

  private void resetCartCheckedOut(Long cartId) {
    if (cartId == null) {
      return;
    }
    cartItemMapper.update(
        null,
        new LambdaUpdateWrapper<CartItem>()
            .eq(CartItem::getCartId, cartId)
            .eq(CartItem::getCheckedOut, 1)
            .set(CartItem::getCheckedOut, 0));
    cartMapper.update(
        null,
        new LambdaUpdateWrapper<Cart>()
            .eq(Cart::getId, cartId)
            .set(Cart::getCartStatus, CART_STATUS_ACTIVE));
  }

  private Long resolveCartId(BusinessActionContext actionContext) {
    if (actionContext == null) {
      return null;
    }
    Object value = actionContext.getActionContext("cartId");
    if (value == null) {
      return null;
    }
    try {
      return Long.valueOf(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private OrderSub buildSubOrder(
      OrderMain mainOrder, CreateMainOrderRequest.CreateSubOrderRequest request) {
    OrderSub subOrder = new OrderSub();
    subOrder.setSubOrderNo("S" + UUID.randomUUID().toString().replace("-", ""));
    subOrder.setMainOrderId(mainOrder.getId());
    subOrder.setMerchantId(request.getMerchantId());
    subOrder.setOrderStatus("CREATED");
    subOrder.setShippingStatus("PENDING");
    subOrder.setAfterSaleStatus("NONE");
    subOrder.setItemAmount(defaultAmount(request.getItemAmount()));
    subOrder.setShippingFee(defaultAmount(request.getShippingFee()));
    subOrder.setDiscountAmount(defaultAmount(request.getDiscountAmount()));
    subOrder.setPayableAmount(defaultAmount(request.getPayableAmount()));
    subOrder.setReceiverName(request.getReceiverName());
    subOrder.setReceiverPhone(request.getReceiverPhone());
    subOrder.setReceiverAddress(request.getReceiverAddress());
    return subOrder;
  }

  private OrderItem buildOrderItem(
      OrderMain mainOrder,
      OrderSub subOrder,
      CreateMainOrderRequest.CreateOrderItemRequest request) {
    OrderItem item = new OrderItem();
    item.setMainOrderId(mainOrder.getId());
    item.setSubOrderId(subOrder.getId());
    item.setSpuId(request.getSpuId());
    item.setSkuId(request.getSkuId());
    item.setSkuCode(request.getSkuCode());
    item.setSkuName(request.getSkuName());
    item.setSkuSnapshot(request.getSkuSnapshot());
    item.setQuantity(request.getQuantity());
    item.setUnitPrice(defaultAmount(request.getUnitPrice()));
    item.setTotalPrice(defaultAmount(request.getTotalPrice()));
    return item;
  }

  private BigDecimal defaultAmount(BigDecimal amount) {
    return amount == null ? BigDecimal.ZERO : amount;
  }

  private <T> T invokeProductService(String action, Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (RpcException ex) {
      throw new RemoteException(
          ResultCode.REMOTE_SERVICE_UNAVAILABLE, "product-service unavailable when " + action, ex);
    }
  }
}
