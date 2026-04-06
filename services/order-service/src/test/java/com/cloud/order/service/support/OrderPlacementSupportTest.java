package com.cloud.order.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.remote.RemoteCallSupport;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.entity.Cart;
import com.cloud.order.entity.CartItem;
import com.cloud.order.mapper.CartItemMapper;
import com.cloud.order.mapper.CartMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderPlacementSupportTest {

  @Mock private CartMapper cartMapper;

  @Mock private CartItemMapper cartItemMapper;

  @Mock private RemoteCallSupport remoteCallSupport;

  @Mock private ProductDubboApi productDubboApi;

  private OrderPlacementSupport orderPlacementSupport;

  @BeforeEach
  void setUp() {
    orderPlacementSupport =
        new OrderPlacementSupport(
            cartMapper, cartItemMapper, remoteCallSupport, new ObjectMapper());
    ReflectionTestUtils.setField(orderPlacementSupport, "productDubboApi", productDubboApi);
    when(remoteCallSupport.query(anyString(), any()))
        .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());
  }

  @Test
  void prepareRequestShouldBuildSnapshotFromCartItems() {
    CreateMainOrderRequest request = new CreateMainOrderRequest();
    request.setUserId(20001L);
    request.setCartId(10L);
    request.setReceiverName("Test User");
    request.setReceiverPhone("13900000001");
    request.setReceiverAddress("Shanghai");

    Cart cart = new Cart();
    cart.setId(10L);
    cart.setUserId(20001L);
    cart.setCartStatus("ACTIVE");
    cart.setDeleted(0);

    CartItem cartItem = new CartItem();
    cartItem.setCartId(10L);
    cartItem.setUserId(20001L);
    cartItem.setSpuId(50001L);
    cartItem.setSkuId(51001L);
    cartItem.setSkuName("Old Cart Name");
    cartItem.setQuantity(2);
    cartItem.setUnitPrice(BigDecimal.valueOf(4999));
    cartItem.setSelected(1);
    cartItem.setCheckedOut(0);
    cartItem.setDeleted(0);

    SpuDetailVO spuDetail = new SpuDetailVO();
    spuDetail.setSpuId(50001L);
    spuDetail.setSpuName("Cloud Phone 15");
    spuDetail.setSubtitle("Balanced flagship");
    spuDetail.setCategoryId(300L);
    spuDetail.setCategoryName("Smart Phone");
    spuDetail.setBrandId(7001L);
    spuDetail.setBrandName("Cloud Mobile");
    spuDetail.setMerchantId(30001L);
    spuDetail.setShopName("Cloud Devices Flagship");
    spuDetail.setMainImage("https://img.example.com/spu-50001.jpg");

    SkuDetailVO skuDetail = new SkuDetailVO();
    skuDetail.setSkuId(51001L);
    skuDetail.setSpuId(50001L);
    skuDetail.setSkuCode("CP15-256-BLK");
    skuDetail.setSkuName("Cloud Phone 15 256G Black");
    skuDetail.setSpecJson("{\"color\":\"black\",\"storage\":\"256G\"}");
    skuDetail.setSalePrice(BigDecimal.valueOf(5299));
    skuDetail.setMarketPrice(BigDecimal.valueOf(5599));
    skuDetail.setImageUrl("https://img.example.com/sku-51001.jpg");

    when(cartMapper.selectById(10L)).thenReturn(cart);
    when(cartItemMapper.selectList(any())).thenReturn(List.of(cartItem));
    when(productDubboApi.getSpuById(50001L)).thenReturn(spuDetail);
    when(productDubboApi.listSkuByIds(List.of(51001L))).thenReturn(List.of(skuDetail));

    orderPlacementSupport.prepareRequest(request);

    assertThat(request.getSubOrders()).hasSize(1);
    CreateMainOrderRequest.CreateOrderItemRequest item =
        request.getSubOrders().get(0).getItems().get(0);
    assertThat(item.getSkuCode()).isEqualTo("CP15-256-BLK");
    assertThat(item.getSkuName()).isEqualTo("Cloud Phone 15 256G Black");
    assertThat(item.getUnitPrice()).isEqualByComparingTo("5299");
    assertThat(item.getTotalPrice()).isEqualByComparingTo("10598");
    assertThat(item.getSkuSnapshot()).contains("\"spuId\":50001");
    assertThat(item.getSkuSnapshot()).contains("\"skuId\":51001");
    assertThat(item.getSkuSnapshot()).contains("\"merchantId\":30001");
    assertThat(item.getSkuSnapshot()).contains("Cloud Phone 15 256G Black");
  }

  @Test
  void prepareRequestShouldBuildSnapshotForDirectCheckout() {
    CreateMainOrderRequest request = new CreateMainOrderRequest();
    request.setUserId(20002L);
    request.setSpuId(50002L);
    request.setSkuId(51003L);
    request.setQuantity(1);
    request.setReceiverName("Test User Two");
    request.setReceiverPhone("13900000002");
    request.setReceiverAddress("Hangzhou");

    SpuDetailVO spuDetail = new SpuDetailVO();
    spuDetail.setSpuId(50002L);
    spuDetail.setSpuName("Cloud Phone 15 Pro");
    spuDetail.setSubtitle("Camera flagship");
    spuDetail.setCategoryId(300L);
    spuDetail.setCategoryName("Smart Phone");
    spuDetail.setBrandId(7001L);
    spuDetail.setBrandName("Cloud Mobile");
    spuDetail.setMerchantId(30001L);
    spuDetail.setShopName("Cloud Devices Flagship");

    SkuDetailVO skuDetail = new SkuDetailVO();
    skuDetail.setSkuId(51003L);
    skuDetail.setSpuId(50002L);
    skuDetail.setSkuCode("CP15P-512-GRY");
    skuDetail.setSkuName("Cloud Phone 15 Pro 512G Gray");
    skuDetail.setSpecJson("{\"color\":\"gray\",\"storage\":\"512G\"}");
    skuDetail.setSalePrice(BigDecimal.valueOf(6999));

    when(productDubboApi.getSpuById(50002L)).thenReturn(spuDetail);
    when(productDubboApi.listSkuByIds(List.of(51003L))).thenReturn(List.of(skuDetail));

    orderPlacementSupport.prepareRequest(request);

    CreateMainOrderRequest.CreateOrderItemRequest item =
        request.getSubOrders().get(0).getItems().get(0);
    assertThat(item.getSkuSnapshot()).contains("\"spuName\":\"Cloud Phone 15 Pro\"");
    assertThat(item.getSkuSnapshot()).contains("\"skuCode\":\"CP15P-512-GRY\"");
    assertThat(item.getUnitPrice()).isEqualByComparingTo("6999");
  }
}
