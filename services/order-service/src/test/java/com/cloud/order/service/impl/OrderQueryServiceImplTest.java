package com.cloud.order.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.api.product.ProductDubboApi;
import com.cloud.api.user.UserDubboApi;
import com.cloud.common.result.PageResult;
import com.cloud.order.dto.OrderSummaryDTO;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceImplTest {

  @Mock private OrderService orderService;
  @Mock private OrderMainMapper orderMainMapper;
  @Mock private OrderSubMapper orderSubMapper;
  @Mock private OrderItemMapper orderItemMapper;
  @Mock private AfterSaleMapper afterSaleMapper;
  @Mock private com.cloud.common.remote.RemoteCallSupport remoteCallSupport;
  @Mock private ProductDubboApi productDubboApi;
  @Mock private UserDubboApi userDubboApi;

  @InjectMocks private OrderQueryServiceImpl orderQueryService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(orderQueryService, "productDubboApi", productDubboApi);
    ReflectionTestUtils.setField(orderQueryService, "userDubboApi", userDubboApi);
    ReflectionTestUtils.setField(orderQueryService, "objectMapper", new ObjectMapper());
  }

  @Test
  void listOrdersLoadsSubOrdersViaIndexedMapperMethod() {
    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(10001L);
    mainOrder.setMainOrderNo("M2026000001");
    mainOrder.setUserId(20001L);
    mainOrder.setOrderStatus("CREATED");

    Page<OrderMain> page = new Page<>(1, 20);
    page.setTotal(1L);
    page.setRecords(List.of(mainOrder));

    OrderSub subOrder = new OrderSub();
    subOrder.setId(11001L);
    subOrder.setMainOrderId(10001L);
    subOrder.setSubOrderNo("S2026000001");
    subOrder.setOrderStatus("CREATED");
    subOrder.setMerchantId(30001L);

    when(orderMainMapper.selectPageActive(any(Page.class), eq(20001L))).thenReturn(page);
    when(orderSubMapper.listActiveByMainOrderIds(List.of(10001L))).thenReturn(List.of(subOrder));
    when(orderItemMapper.listActiveBySubOrderIds(List.of(11001L))).thenReturn(List.of());

    PageResult<OrderSummaryDTO> result =
        orderQueryService.listOrders(userAuthentication(20001L), 1, 20, null, null, null);

    assertEquals(1L, result.getTotal());
    assertEquals(1, result.getRecords().size());
    assertEquals(11001L, result.getRecords().get(0).getSubOrderId());
    verify(orderSubMapper).listActiveByMainOrderIds(List.of(10001L));
    verify(orderSubMapper, never()).selectList(any());
  }

  @Test
  void listOrdersLoadsLatestAfterSaleViaIndexedMapperMethod() {
    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(10001L);
    mainOrder.setMainOrderNo("M2026000001");
    mainOrder.setUserId(20001L);
    mainOrder.setOrderStatus("DONE");

    Page<OrderMain> page = new Page<>(1, 20);
    page.setTotal(1L);
    page.setRecords(List.of(mainOrder));

    OrderSub subOrder = new OrderSub();
    subOrder.setId(11001L);
    subOrder.setMainOrderId(10001L);
    subOrder.setSubOrderNo("S2026000001");
    subOrder.setOrderStatus("DONE");
    subOrder.setMerchantId(30001L);
    subOrder.setAfterSaleStatus("REFUNDING");

    AfterSale afterSale = new AfterSale();
    afterSale.setId(14001L);
    afterSale.setAfterSaleNo("AS2026000001");
    afterSale.setAfterSaleType("REFUND");

    when(orderMainMapper.selectPageActive(any(Page.class), eq(20001L))).thenReturn(page);
    when(orderSubMapper.listActiveByMainOrderIds(List.of(10001L))).thenReturn(List.of(subOrder));
    when(orderItemMapper.listActiveBySubOrderIds(List.of(11001L))).thenReturn(List.of());
    when(afterSaleMapper.selectLatestActiveBySubOrderId(11001L)).thenReturn(afterSale);

    PageResult<OrderSummaryDTO> result =
        orderQueryService.listOrders(userAuthentication(20001L), 1, 20, null, null, null);

    assertEquals(14001L, result.getRecords().get(0).getAfterSaleId());
    assertEquals("AS2026000001", result.getRecords().get(0).getAfterSaleNo());
    verify(afterSaleMapper).selectLatestActiveBySubOrderId(11001L);
    verify(afterSaleMapper, never()).selectOne(any());
  }

  @Test
  void requireAccessibleMainOrderUsesIndexedMerchantOwnershipCheck() {
    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(10001L);
    mainOrder.setUserId(20001L);
    mainOrder.setDeleted(0);

    JwtAuthenticationToken authentication =
        authentication(30001L, AuthorityUtils.createAuthorityList("ROLE_MERCHANT"));

    when(orderService.getMainOrder(10001L)).thenReturn(mainOrder);
    when(userDubboApi.findMerchantIdByOwnerUserId(30001L)).thenReturn(30001L);
    when(orderSubMapper.countActiveByMainOrderIdAndMerchantId(10001L, 30001L)).thenReturn(1L);

    OrderMain result = orderQueryService.requireAccessibleMainOrder(10001L, authentication);

    assertSame(mainOrder, result);
    verify(orderSubMapper).countActiveByMainOrderIdAndMerchantId(10001L, 30001L);
    verify(orderSubMapper, never()).selectCount(any());
  }

  private JwtAuthenticationToken userAuthentication(Long userId) {
    return authentication(userId, AuthorityUtils.createAuthorityList("ROLE_USER"));
  }

  private JwtAuthenticationToken authentication(
      Long userId, List<org.springframework.security.core.GrantedAuthority> authorities) {
    Jwt jwt =
        Jwt.withTokenValue("token-" + userId)
            .header("alg", "none")
            .claim("user_id", String.valueOf(userId))
            .build();
    return new JwtAuthenticationToken(jwt, authorities);
  }
}
