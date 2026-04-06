package com.cloud.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.remote.RemoteCallSupport;
import com.cloud.order.dto.CartDTO;
import com.cloud.order.dto.CartSyncRequest;
import com.cloud.order.entity.Cart;
import com.cloud.order.entity.CartItem;
import com.cloud.order.mapper.CartItemMapper;
import com.cloud.order.mapper.CartMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

  @Mock private CartMapper cartMapper;
  @Mock private CartItemMapper cartItemMapper;
  @Mock private RemoteCallSupport remoteCallSupport;
  @Mock private ProductDubboApi productDubboApi;

  private CartServiceImpl cartService;

  @BeforeEach
  void setUp() {
    cartService = new CartServiceImpl(cartMapper, cartItemMapper, remoteCallSupport);
    ReflectionTestUtils.setField(cartService, "productDubboApi", productDubboApi);
    lenient()
        .when(remoteCallSupport.queryOrFallback(any(), any(), any()))
        .thenAnswer(
            invocation -> invocation.getArgument(1, java.util.function.Supplier.class).get());
  }

  @Test
  void getCurrentCartShouldReturnEmptyCartWhenNoActiveCartExists() {
    when(cartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

    CartDTO cart = cartService.getCurrentCart(101L);

    assertThat(cart.getId()).isNull();
    assertThat(cart.getUserId()).isEqualTo(101L);
    assertThat(cart.getItems()).isEmpty();
    assertThat(cart.getSelectedCount()).isZero();
    assertThat(cart.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void syncCurrentCartShouldCreateCartAndReturnMappedItems() {
    when(cartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

    ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
    when(cartMapper.insert(cartCaptor.capture()))
        .thenAnswer(
            invocation -> {
              Cart cart = cartCaptor.getValue();
              cart.setId(501L);
              return 1;
            });

    when(cartItemMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(), List.of(existingItem(501L, 1001L, 2001L)));

    ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
    when(cartItemMapper.insert(itemCaptor.capture()))
        .thenAnswer(
            invocation -> {
              CartItem item = itemCaptor.getValue();
              item.setId(701L);
              return 1;
            });

    SpuDetailVO spuDetail = new SpuDetailVO();
    spuDetail.setSpuId(1001L);
    spuDetail.setSpuName("Cloud Phone");
    spuDetail.setMerchantId(3001L);
    when(productDubboApi.getSpuById(1001L)).thenReturn(spuDetail);

    CartSyncRequest request = new CartSyncRequest();
    CartSyncRequest.CartSyncItemRequest itemRequest = new CartSyncRequest.CartSyncItemRequest();
    itemRequest.setSpuId(1001L);
    itemRequest.setSkuId(2001L);
    itemRequest.setSkuName("Cloud Phone Pro");
    itemRequest.setUnitPrice(BigDecimal.valueOf(199));
    itemRequest.setQuantity(2);
    itemRequest.setSelected(1);
    request.setItems(List.of(itemRequest));

    CartDTO cart = cartService.syncCart(101L, request);

    assertThat(cart.getId()).isEqualTo(501L);
    assertThat(cart.getSelectedCount()).isEqualTo(1);
    assertThat(cart.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(398));
    assertThat(cart.getItems()).hasSize(1);
    assertThat(cart.getItems().get(0).getShopId()).isEqualTo(3001L);
    assertThat(cart.getItems().get(0).getProductName()).isEqualTo("Cloud Phone");
    verify(cartMapper).updateById(any(Cart.class));
  }

  private CartItem existingItem(Long cartId, Long spuId, Long skuId) {
    CartItem item = new CartItem();
    item.setId(701L);
    item.setCartId(cartId);
    item.setUserId(101L);
    item.setSpuId(spuId);
    item.setSkuId(skuId);
    item.setSkuName("Cloud Phone Pro");
    item.setUnitPrice(BigDecimal.valueOf(199));
    item.setQuantity(2);
    item.setSelected(1);
    item.setCheckedOut(0);
    return item;
  }
}
