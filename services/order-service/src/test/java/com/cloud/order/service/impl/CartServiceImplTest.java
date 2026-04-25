package com.cloud.order.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.remote.RemoteCallSupport;
import com.cloud.order.dto.CartDTO;
import com.cloud.order.dto.CartSyncRequest;
import com.cloud.order.entity.Cart;
import com.cloud.order.entity.CartItem;
import com.cloud.order.mapper.CartItemMapper;
import com.cloud.order.mapper.CartMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

  @Mock private CartMapper cartMapper;
  @Mock private CartItemMapper cartItemMapper;
  @Mock private RemoteCallSupport remoteCallSupport;

  @InjectMocks private CartServiceImpl cartService;

  @Test
  void syncCartReusesCheckedOutItemWithSameUserAndSku() {
    Cart cart = new Cart();
    cart.setId(100L);
    cart.setUserId(88L);
    cart.setCartStatus("ACTIVE");
    when(cartMapper.selectActiveByUserId(88L)).thenReturn(cart);
    when(cartItemMapper.listActiveByCartIdAndUserId(100L, 88L))
        .thenReturn(List.of(), List.of(reusedCartItem()));
    when(cartItemMapper.selectByUserIdAndSkuIds(88L, List.of(51002L)))
        .thenReturn(List.of(oldCheckedOutItem()));
    when(remoteCallSupport.queryOrFallback(anyString(), any(Supplier.class), any(Function.class)))
        .thenReturn(null);

    CartSyncRequest request = new CartSyncRequest();
    CartSyncRequest.CartSyncItemRequest item = new CartSyncRequest.CartSyncItemRequest();
    item.setSpuId(50001L);
    item.setSkuId(51002L);
    item.setSkuName("Cloud Phone 15 512G Silver");
    item.setUnitPrice(new BigDecimal("5699.00"));
    item.setQuantity(1);
    item.setSelected(1);
    request.setItems(List.of(item));

    CartDTO result = cartService.syncCart(88L, request);

    ArgumentCaptor<CartItem> updatedCaptor = ArgumentCaptor.forClass(CartItem.class);
    verify(cartItemMapper).updateById(updatedCaptor.capture());
    CartItem updated = updatedCaptor.getValue();
    assertEquals(900L, updated.getId());
    assertEquals(100L, updated.getCartId());
    assertEquals(0, updated.getCheckedOut());
    assertEquals(1, updated.getSelected());
    assertEquals(new BigDecimal("5699.00"), updated.getUnitPrice());
    verify(cartItemMapper, never()).insert(any(CartItem.class));
    verify(cartMapper).updateById(eq(cart));
    assertEquals(1, result.getItems().size());
    assertEquals(100L, result.getId());
  }

  private CartItem oldCheckedOutItem() {
    CartItem item = new CartItem();
    item.setId(900L);
    item.setCartId(50L);
    item.setUserId(88L);
    item.setSpuId(50001L);
    item.setSkuId(51002L);
    item.setSkuName("Old Item");
    item.setUnitPrice(new BigDecimal("4999.00"));
    item.setQuantity(1);
    item.setSelected(0);
    item.setCheckedOut(1);
    return item;
  }

  private CartItem reusedCartItem() {
    CartItem item = new CartItem();
    item.setId(900L);
    item.setCartId(100L);
    item.setUserId(88L);
    item.setSpuId(50001L);
    item.setSkuId(51002L);
    item.setSkuName("Cloud Phone 15 512G Silver");
    item.setUnitPrice(new BigDecimal("5699.00"));
    item.setQuantity(1);
    item.setSelected(1);
    item.setCheckedOut(0);
    return item;
  }
}
