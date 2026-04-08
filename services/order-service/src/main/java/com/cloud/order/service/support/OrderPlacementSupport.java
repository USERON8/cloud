package com.cloud.order.service.support;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.exception.BizException;
import com.cloud.common.remote.RemoteCallSupport;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.entity.Cart;
import com.cloud.order.entity.CartItem;
import com.cloud.order.mapper.CartItemMapper;
import com.cloud.order.mapper.CartMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderPlacementSupport {

  private static final String CART_STATUS_ACTIVE = "ACTIVE";
  private static final String CART_STATUS_CHECKED_OUT = "CHECKED_OUT";

  private final CartMapper cartMapper;
  private final CartItemMapper cartItemMapper;
  private final RemoteCallSupport remoteCallSupport;
  private final ObjectMapper objectMapper;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private ProductDubboApi productDubboApi;

  public void prepareRequest(CreateMainOrderRequest request) {
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
  }

  @Transactional(rollbackFor = Exception.class)
  public void markCartCheckedOut(Long cartId, Long userId) {
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

  private void buildFromCart(CreateMainOrderRequest request) {
    Long cartId = request.getCartId();
    if (cartId == null) {
      return;
    }
    if (request.getUserId() == null) {
      throw new BizException("user id is required for cart checkout");
    }
    Cart cart = cartMapper.selectById(cartId);
    if ((cart == null || cart.getDeleted() == 1) && request.getUserId() != null) {
      // Browser clients lose precision for 64-bit cart ids, so tolerate an imprecise cartId
      // by falling back to the current user's active cart and correcting the request in-place.
      cart = cartMapper.selectActiveByUserId(request.getUserId());
      if (cart != null) {
        request.setCartId(cart.getId());
      }
    }
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
        cartItemMapper.listActiveByCartIdAndUserId(request.getCartId(), request.getUserId());
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
        SkuDetailVO skuDetail = skuDetails.get(cartItem.getSkuId());
        BigDecimal unitPrice = cartItem.getUnitPrice();
        if (skuDetail != null && skuDetail.getSalePrice() != null) {
          unitPrice = skuDetail.getSalePrice();
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
        if (skuDetail != null) {
          item.setSkuCode(skuDetail.getSkuCode());
          if (skuDetail.getSkuName() != null) {
            item.setSkuName(skuDetail.getSkuName());
          }
        }
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setTotalPrice(totalPrice);
        item.setSkuSnapshot(
            buildSkuSnapshot(spuDetails.get(cartItem.getSpuId()), skuDetail, unitPrice, quantity));
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
    item.setSkuSnapshot(buildSkuSnapshot(spuDetail, skuDetail, unitPrice, quantity));

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

  private <T> T invokeProductService(String action, Supplier<T> supplier) {
    return remoteCallSupport.query("product-service." + action, supplier);
  }

  private String buildSkuSnapshot(
      SpuDetailVO spuDetail, SkuDetailVO skuDetail, BigDecimal unitPrice, Integer quantity) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    if (spuDetail != null) {
      snapshot.put("spuId", spuDetail.getSpuId());
      snapshot.put("spuName", spuDetail.getSpuName());
      snapshot.put("subtitle", spuDetail.getSubtitle());
      snapshot.put("categoryId", spuDetail.getCategoryId());
      snapshot.put("categoryName", spuDetail.getCategoryName());
      snapshot.put("brandId", spuDetail.getBrandId());
      snapshot.put("brandName", spuDetail.getBrandName());
      snapshot.put("merchantId", spuDetail.getMerchantId());
      snapshot.put("shopName", spuDetail.getShopName());
      snapshot.put("spuMainImage", spuDetail.getMainImage());
    }
    if (skuDetail != null) {
      snapshot.put("skuId", skuDetail.getSkuId());
      snapshot.put("skuCode", skuDetail.getSkuCode());
      snapshot.put("skuName", skuDetail.getSkuName());
      snapshot.put("specJson", skuDetail.getSpecJson());
      snapshot.put("marketPrice", skuDetail.getMarketPrice());
      snapshot.put("imageUrl", skuDetail.getImageUrl());
      snapshot.put("imageFile", skuDetail.getImageFile());
    }
    snapshot.put("unitPrice", unitPrice);
    snapshot.put("quantity", quantity);
    try {
      return objectMapper.writeValueAsString(snapshot);
    } catch (JsonProcessingException ex) {
      throw new BizException("failed to serialize sku snapshot");
    }
  }
}
