package com.cloud.order.service.impl;

import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.exception.BizException;
import com.cloud.common.remote.RemoteCallSupport;
import com.cloud.order.dto.CartDTO;
import com.cloud.order.dto.CartItemDTO;
import com.cloud.order.dto.CartSyncRequest;
import com.cloud.order.entity.Cart;
import com.cloud.order.entity.CartItem;
import com.cloud.order.mapper.CartItemMapper;
import com.cloud.order.mapper.CartMapper;
import com.cloud.order.service.CartService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

  private static final String CART_STATUS_ACTIVE = "ACTIVE";

  private final CartMapper cartMapper;
  private final CartItemMapper cartItemMapper;
  private final RemoteCallSupport remoteCallSupport;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private ProductDubboApi productDubboApi;

  @Override
  public CartDTO getCurrentCart(Long userId) {
    Long safeUserId = requireUserId(userId);
    Cart cart = findOrCreateActiveCart(safeUserId, false);
    if (cart == null) {
      CartDTO empty = new CartDTO();
      empty.setUserId(safeUserId);
      empty.setCartStatus(CART_STATUS_ACTIVE);
      empty.setSelectedCount(0);
      empty.setTotalAmount(BigDecimal.ZERO);
      empty.setItems(List.of());
      return empty;
    }
    return toCartDto(cart, listActiveItems(cart.getId(), safeUserId));
  }

  @Override
  public CartDTO syncCart(Long userId, CartSyncRequest request) {
    Long safeUserId = requireUserId(userId);
    Cart cart = findOrCreateActiveCart(safeUserId, true);
    List<CartSyncRequest.CartSyncItemRequest> requests =
        request == null || request.getItems() == null ? List.of() : request.getItems();
    Map<Long, CartSyncRequest.CartSyncItemRequest> requestBySkuId = normalizeRequests(requests);
    List<CartItem> activeItems = listActiveItems(cart.getId(), safeUserId);
    Map<Long, CartItem> activeBySkuId = new HashMap<>();
    for (CartItem activeItem : activeItems) {
      activeBySkuId.put(activeItem.getSkuId(), activeItem);
    }
    Map<Long, CartItem> existingBySkuId =
        loadExistingItemsBySkuId(safeUserId, new ArrayList<>(requestBySkuId.keySet()));

    for (CartSyncRequest.CartSyncItemRequest itemRequest : requestBySkuId.values()) {
      CartItem existing = existingBySkuId.get(itemRequest.getSkuId());
      if (existing == null) {
        CartItem created = new CartItem();
        created.setCartId(cart.getId());
        created.setUserId(safeUserId);
        created.setSpuId(itemRequest.getSpuId());
        created.setSkuId(itemRequest.getSkuId());
        created.setSkuName(itemRequest.getSkuName().trim());
        created.setUnitPrice(itemRequest.getUnitPrice());
        created.setQuantity(itemRequest.getQuantity());
        created.setSelected(normalizeSelected(itemRequest.getSelected()));
        created.setCheckedOut(0);
        cartItemMapper.insert(created);
        continue;
      }
      existing.setCartId(cart.getId());
      existing.setSpuId(itemRequest.getSpuId());
      existing.setSkuName(itemRequest.getSkuName().trim());
      existing.setUnitPrice(itemRequest.getUnitPrice());
      existing.setQuantity(itemRequest.getQuantity());
      existing.setSelected(normalizeSelected(itemRequest.getSelected()));
      existing.setCheckedOut(0);
      cartItemMapper.updateById(existing);
      activeBySkuId.remove(itemRequest.getSkuId());
    }

    for (CartItem staleItem : activeBySkuId.values()) {
      cartItemMapper.deleteById(staleItem.getId());
    }

    List<CartItem> syncedItems = listActiveItems(cart.getId(), safeUserId);
    refreshCartSummary(cart, syncedItems);
    return toCartDto(cart, syncedItems);
  }

  private Map<Long, CartSyncRequest.CartSyncItemRequest> normalizeRequests(
      List<CartSyncRequest.CartSyncItemRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, CartSyncRequest.CartSyncItemRequest> normalized = new LinkedHashMap<>();
    for (CartSyncRequest.CartSyncItemRequest request : requests) {
      if (request == null || request.getSkuId() == null) {
        continue;
      }
      if (request.getQuantity() == null || request.getQuantity() <= 0) {
        throw new BizException("cart item quantity must be greater than 0");
      }
      if (request.getSpuId() == null) {
        throw new BizException("cart item spuId is required");
      }
      if (request.getSkuName() == null || request.getSkuName().isBlank()) {
        throw new BizException("cart item skuName is required");
      }
      if (request.getUnitPrice() == null || request.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
        throw new BizException("cart item unitPrice must be greater than or equal to 0");
      }
      normalized.put(request.getSkuId(), request);
    }
    return normalized;
  }

  private Cart findOrCreateActiveCart(Long userId, boolean createIfMissing) {
    Cart cart = cartMapper.selectActiveByUserId(userId);
    if (cart != null || !createIfMissing) {
      return cart;
    }
    Cart created = new Cart();
    created.setCartNo(buildCartNo(userId));
    created.setUserId(userId);
    created.setCartStatus(CART_STATUS_ACTIVE);
    created.setSelectedCount(0);
    created.setTotalAmount(BigDecimal.ZERO);
    cartMapper.insert(created);
    return created;
  }

  private List<CartItem> listActiveItems(Long cartId, Long userId) {
    if (cartId == null || userId == null) {
      return List.of();
    }
    List<CartItem> items = cartItemMapper.listActiveByCartIdAndUserId(cartId, userId);
    return items == null ? List.of() : items;
  }

  private Map<Long, CartItem> loadExistingItemsBySkuId(Long userId, List<Long> skuIds) {
    if (userId == null || skuIds == null || skuIds.isEmpty()) {
      return Map.of();
    }
    List<CartItem> items = cartItemMapper.selectByUserIdAndSkuIds(userId, skuIds);
    if (items == null || items.isEmpty()) {
      return Map.of();
    }
    Map<Long, CartItem> existingBySkuId = new HashMap<>();
    for (CartItem item : items) {
      if (item != null && item.getSkuId() != null) {
        existingBySkuId.put(item.getSkuId(), item);
      }
    }
    return existingBySkuId;
  }

  private void refreshCartSummary(Cart cart, List<CartItem> items) {
    int selectedCount = 0;
    BigDecimal totalAmount = BigDecimal.ZERO;
    for (CartItem item : items) {
      if (normalizeSelected(item.getSelected()) == 1) {
        selectedCount += 1;
        totalAmount =
            totalAmount.add(
                defaultAmount(item.getUnitPrice())
                    .multiply(
                        BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity())));
      }
    }
    cart.setSelectedCount(selectedCount);
    cart.setTotalAmount(totalAmount);
    cartMapper.updateById(cart);
  }

  private CartDTO toCartDto(Cart cart, List<CartItem> items) {
    CartDTO dto = new CartDTO();
    dto.setId(cart.getId());
    dto.setCartNo(cart.getCartNo());
    dto.setUserId(cart.getUserId());
    dto.setCartStatus(cart.getCartStatus());
    dto.setSelectedCount(cart.getSelectedCount());
    dto.setTotalAmount(defaultAmount(cart.getTotalAmount()));
    dto.setItems(toItemDtos(items));
    return dto;
  }

  private List<CartItemDTO> toItemDtos(List<CartItem> items) {
    if (items == null || items.isEmpty()) {
      return List.of();
    }
    Map<Long, SpuDetailVO> spuDetails = loadSpuDetails(items);
    List<CartItemDTO> result = new ArrayList<>(items.size());
    for (CartItem item : items) {
      CartItemDTO dto = new CartItemDTO();
      dto.setId(item.getId());
      dto.setCartId(item.getCartId());
      dto.setSpuId(item.getSpuId());
      dto.setSkuId(item.getSkuId());
      dto.setSkuName(item.getSkuName());
      dto.setUnitPrice(defaultAmount(item.getUnitPrice()));
      dto.setQuantity(item.getQuantity());
      dto.setSelected(normalizeSelected(item.getSelected()));
      dto.setCheckedOut(item.getCheckedOut());
      SpuDetailVO spuDetail = spuDetails.get(item.getSpuId());
      if (spuDetail != null) {
        dto.setShopId(spuDetail.getMerchantId());
        dto.setProductName(
            spuDetail.getSpuName() == null || spuDetail.getSpuName().isBlank()
                ? item.getSkuName()
                : spuDetail.getSpuName());
      } else {
        dto.setProductName(item.getSkuName());
      }
      result.add(dto);
    }
    return result;
  }

  private Map<Long, SpuDetailVO> loadSpuDetails(List<CartItem> items) {
    Map<Long, SpuDetailVO> result = new HashMap<>();
    for (CartItem item : items) {
      if (item.getSpuId() == null || result.containsKey(item.getSpuId())) {
        continue;
      }
      SpuDetailVO spuDetail =
          remoteCallSupport.queryOrFallback(
              "product-service.get spu by id",
              () -> productDubboApi.getSpuById(item.getSpuId()),
              ex -> null);
      if (spuDetail != null && spuDetail.getSpuId() != null) {
        result.put(spuDetail.getSpuId(), spuDetail);
      }
    }
    return result;
  }

  private int normalizeSelected(Integer selected) {
    return Objects.equals(selected, 0) ? 0 : 1;
  }

  private BigDecimal defaultAmount(BigDecimal amount) {
    return amount == null ? BigDecimal.ZERO : amount;
  }

  private Long requireUserId(Long userId) {
    if (userId == null) {
      throw new BizException("user id is required");
    }
    return userId;
  }

  private String buildCartNo(Long userId) {
    return "CART-" + userId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }
}
