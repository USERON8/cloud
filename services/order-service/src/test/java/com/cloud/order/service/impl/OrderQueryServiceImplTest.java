package com.cloud.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.common.remote.RemoteCallSupport;
import com.cloud.order.dto.OrderSummaryDTO;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  @Mock private RemoteCallSupport remoteCallSupport;
  @Mock private ProductDubboApi productDubboApi;

  private OrderQueryServiceImpl orderQueryService;

  @BeforeEach
  void setUp() {
    orderQueryService =
        new OrderQueryServiceImpl(
            orderService,
            orderMainMapper,
            orderSubMapper,
            orderItemMapper,
            afterSaleMapper,
            remoteCallSupport,
            new ObjectMapper());
    ReflectionTestUtils.setField(orderQueryService, "productDubboApi", productDubboApi);
  }

  @Test
  void getOrderSummaryShouldExposeSingleSubOrderMetadata() {
    OrderMain main = new OrderMain();
    main.setId(10L);
    main.setMainOrderNo("M-10");
    main.setUserId(101L);
    main.setTotalAmount(BigDecimal.valueOf(88));
    main.setPayableAmount(BigDecimal.valueOf(80));
    main.setDeleted(0);

    OrderSub sub = new OrderSub();
    sub.setId(20L);
    sub.setMainOrderId(10L);
    sub.setSubOrderNo("S-20");
    sub.setMerchantId(300L);
    sub.setOrderStatus("PAID");
    sub.setAfterSaleStatus("NONE");

    when(orderService.getMainOrder(10L)).thenReturn(main);
    when(orderService.listSubOrders(10L)).thenReturn(List.of(sub));
    when(orderItemMapper.listActiveBySubOrderIds(List.of(20L)))
        .thenReturn(
            List.of(
                orderItem(201L, 20L, 50001L, 51001L, "{\"spuName\":\"Phone\",\"unitPrice\":80}")));
    when(remoteCallSupport.queryOrFallback(any(), any(), any()))
        .thenAnswer(
            invocation -> invocation.getArgument(1, java.util.function.Supplier.class).get());
    AfterSale afterSale = new AfterSale();
    afterSale.setId(30L);
    afterSale.setAfterSaleNo("AS-30");
    afterSale.setAfterSaleType("RETURN_REFUND");
    when(afterSaleMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(afterSale);
    SkuDetailVO skuDetail = new SkuDetailVO();
    skuDetail.setSkuId(51001L);
    skuDetail.setSpuId(50001L);
    skuDetail.setSkuCode("SKU-51001");
    skuDetail.setSkuName("Phone Pro");
    skuDetail.setSpecJson("{\"color\":\"black\"}");
    skuDetail.setSalePrice(BigDecimal.valueOf(79));
    skuDetail.setMarketPrice(BigDecimal.valueOf(99));
    skuDetail.setImageUrl("https://img.example.com/51001.png");
    skuDetail.setStatus(1);
    SpuDetailVO spuDetail = new SpuDetailVO();
    spuDetail.setSpuId(50001L);
    spuDetail.setSpuName("Phone");
    spuDetail.setBrandName("Cloud");
    spuDetail.setCategoryName("Mobile");
    spuDetail.setMerchantId(300L);
    spuDetail.setShopName("Cloud Shop");
    when(productDubboApi.listSkuByIds(List.of(51001L))).thenReturn(List.of(skuDetail));
    when(productDubboApi.getSpuById(50001L)).thenReturn(spuDetail);

    OrderSummaryDTO summary =
        orderQueryService.getOrderSummary(10L, authentication("1", "ROLE_ADMIN", "order:query"));

    assertThat(summary.getId()).isEqualTo(10L);
    assertThat(summary.getOrderNo()).isEqualTo("M-10");
    assertThat(summary.getSubOrderId()).isEqualTo(20L);
    assertThat(summary.getSubOrderNo()).isEqualTo("S-20");
    assertThat(summary.getMerchantId()).isEqualTo(300L);
    assertThat(summary.getAfterSaleId()).isEqualTo(30L);
    assertThat(summary.getAfterSaleNo()).isEqualTo("AS-30");
    assertThat(summary.getAfterSaleType()).isEqualTo("RETURN_REFUND");
    assertThat(summary.getRefundNo()).isEqualTo("RFAS-30");
    assertThat(summary.getAfterSaleStatus()).isEqualTo("NONE");
    assertThat(summary.getStatus()).isEqualTo(1);
    assertThat(summary.getItems()).hasSize(1);
    assertThat(summary.getItems().get(0).getSkuSnapshot()).containsEntry("spuName", "Phone");
    assertThat(summary.getItems().get(0).getLatestProduct()).isNotNull();
    assertThat(summary.getItems().get(0).getLatestProduct().getSkuName()).isEqualTo("Phone Pro");
    assertThat(summary.getItems().get(0).getLatestProduct().getBrandName()).isEqualTo("Cloud");
  }

  @Test
  void getOrderSummaryShouldLeaveSubOrderMetadataBlankForMultiSubOrderMain() {
    OrderMain main = new OrderMain();
    main.setId(11L);
    main.setMainOrderNo("M-11");
    main.setUserId(101L);
    main.setTotalAmount(BigDecimal.valueOf(120));
    main.setPayableAmount(BigDecimal.valueOf(120));
    main.setDeleted(0);

    OrderSub first = new OrderSub();
    first.setId(21L);
    first.setMainOrderId(11L);
    first.setSubOrderNo("S-21");
    first.setMerchantId(301L);
    first.setOrderStatus("PAID");
    first.setAfterSaleStatus("NONE");

    OrderSub second = new OrderSub();
    second.setId(22L);
    second.setMainOrderId(11L);
    second.setSubOrderNo("S-22");
    second.setMerchantId(302L);
    second.setOrderStatus("SHIPPED");
    second.setAfterSaleStatus("APPLIED");

    when(orderService.getMainOrder(11L)).thenReturn(main);
    when(orderService.listSubOrders(11L)).thenReturn(List.of(first, second));
    when(orderItemMapper.listActiveBySubOrderIds(List.of(21L, 22L))).thenReturn(List.of());

    OrderSummaryDTO summary =
        orderQueryService.getOrderSummary(11L, authentication("1", "ROLE_ADMIN", "order:query"));

    assertThat(summary.getSubOrderId()).isNull();
    assertThat(summary.getSubOrderNo()).isNull();
    assertThat(summary.getMerchantId()).isNull();
    assertThat(summary.getAfterSaleId()).isNull();
    assertThat(summary.getAfterSaleNo()).isNull();
    assertThat(summary.getRefundNo()).isNull();
    assertThat(summary.getAfterSaleStatus()).isNull();
    assertThat(summary.getStatus()).isEqualTo(2);
  }

  @Test
  void listOrdersShouldProjectMerchantOwnedSubOrderMetadata() {
    OrderMain main = new OrderMain();
    main.setId(12L);
    main.setMainOrderNo("M-12");
    main.setUserId(900L);
    main.setTotalAmount(BigDecimal.valueOf(220));
    main.setPayableAmount(BigDecimal.valueOf(200));

    OrderSub ownSub = new OrderSub();
    ownSub.setId(41L);
    ownSub.setMainOrderId(12L);
    ownSub.setSubOrderNo("S-41");
    ownSub.setMerchantId(301L);
    ownSub.setOrderStatus("PAID");
    ownSub.setAfterSaleStatus("APPLIED");

    OrderSub otherSub = new OrderSub();
    otherSub.setId(42L);
    otherSub.setMainOrderId(12L);
    otherSub.setSubOrderNo("S-42");
    otherSub.setMerchantId(302L);
    otherSub.setOrderStatus("SHIPPED");
    otherSub.setAfterSaleStatus("NONE");

    Page<OrderMain> page = new Page<>(1, 20);
    page.setRecords(List.of(main));
    page.setTotal(1);

    AfterSale afterSale = new AfterSale();
    afterSale.setId(61L);
    afterSale.setAfterSaleNo("AS-61");
    afterSale.setAfterSaleType("REFUND");

    when(orderMainMapper.selectPageByMerchant(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(301L),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.isNull()))
        .thenReturn(page);
    when(orderSubMapper.selectList(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(ownSub, otherSub));
    when(orderItemMapper.listActiveBySubOrderIds(List.of(41L)))
        .thenReturn(List.of(orderItem(301L, 41L, 60001L, 61001L, "{\"skuName\":\"Headset\"}")));
    when(afterSaleMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(afterSale);

    var result =
        orderQueryService.listOrders(
            authentication("301", "ROLE_MERCHANT", "order:query"), 1, 20, null, null, null);

    assertThat(result.getRecords()).hasSize(1);
    OrderSummaryDTO summary = result.getRecords().get(0);
    assertThat(summary.getSubOrderId()).isEqualTo(41L);
    assertThat(summary.getSubOrderNo()).isEqualTo("S-41");
    assertThat(summary.getMerchantId()).isEqualTo(301L);
    assertThat(summary.getAfterSaleId()).isEqualTo(61L);
    assertThat(summary.getAfterSaleNo()).isEqualTo("AS-61");
    assertThat(summary.getAfterSaleType()).isEqualTo("REFUND");
    assertThat(summary.getRefundNo()).isEqualTo("RFAS-61");
    assertThat(summary.getAfterSaleStatus()).isEqualTo("APPLIED");
    assertThat(summary.getStatus()).isEqualTo(1);
    assertThat(summary.getItems()).hasSize(1);
    assertThat(summary.getItems().get(0).getSkuSnapshot()).containsEntry("skuName", "Headset");
  }

  @Test
  void listOrdersShouldUseVisibleSubOrderStatusesForPaidFilter() {
    OrderMain main = new OrderMain();
    main.setId(21L);
    main.setMainOrderNo("M-21");
    main.setUserId(901L);
    main.setTotalAmount(BigDecimal.valueOf(120));
    main.setPayableAmount(BigDecimal.valueOf(120));

    OrderSub paidSub = new OrderSub();
    paidSub.setId(51L);
    paidSub.setMainOrderId(21L);
    paidSub.setSubOrderNo("S-51");
    paidSub.setMerchantId(401L);
    paidSub.setOrderStatus("PAID");
    paidSub.setAfterSaleStatus("NONE");

    Page<OrderMain> page = new Page<>(1, 20);
    page.setRecords(List.of(main));
    page.setTotal(1);

    when(orderMainMapper.selectPageByVisibleStatus(any(), eq(null), eq(901L), eq(1)))
        .thenReturn(page);
    when(orderSubMapper.selectList(any())).thenReturn(List.of(paidSub));
    when(orderItemMapper.listActiveBySubOrderIds(List.of(51L))).thenReturn(List.of());

    var result =
        orderQueryService.listOrders(
            authentication("901", "ROLE_USER", "order:query"), 1, 20, null, null, 1);

    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getRecords()).hasSize(1);
    assertThat(result.getRecords().get(0).getStatus()).isEqualTo(1);
    verify(orderMainMapper).selectPageByVisibleStatus(any(), eq(null), eq(901L), eq(1));
  }

  @Test
  void listOrdersShouldUseVisibleSubOrderStatusesForShippedFilter() {
    OrderMain main = new OrderMain();
    main.setId(22L);
    main.setMainOrderNo("M-22");
    main.setUserId(902L);
    main.setTotalAmount(BigDecimal.valueOf(180));
    main.setPayableAmount(BigDecimal.valueOf(160));

    OrderSub shippedSub = new OrderSub();
    shippedSub.setId(52L);
    shippedSub.setMainOrderId(22L);
    shippedSub.setSubOrderNo("S-52");
    shippedSub.setMerchantId(402L);
    shippedSub.setOrderStatus("SHIPPED");
    shippedSub.setAfterSaleStatus("NONE");

    Page<OrderMain> page = new Page<>(1, 20);
    page.setRecords(List.of(main));
    page.setTotal(1);

    when(orderMainMapper.selectPageByVisibleStatus(any(), eq(null), eq(902L), eq(2)))
        .thenReturn(page);
    when(orderSubMapper.selectList(any())).thenReturn(List.of(shippedSub));
    when(orderItemMapper.listActiveBySubOrderIds(List.of(52L)))
        .thenReturn(
            List.of(
                orderItem(401L, 52L, 70001L, 71001L, "{\"skuName\":\"Tablet\",\"skuId\":71001}")));

    var result =
        orderQueryService.listOrders(
            authentication("902", "ROLE_USER", "order:query"), 1, 20, null, null, 2);

    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getRecords()).hasSize(1);
    assertThat(result.getRecords().get(0).getStatus()).isEqualTo(2);
    assertThat(result.getRecords().get(0).getItems()).hasSize(1);
    assertThat(result.getRecords().get(0).getItems().get(0).getLatestProduct()).isNull();
    assertThat(result.getRecords().get(0).getItems().get(0).getSkuSnapshot())
        .containsEntry("skuName", "Tablet");
    verify(orderMainMapper).selectPageByVisibleStatus(any(), eq(null), eq(902L), eq(2));
  }

  private OrderItem orderItem(
      Long id, Long subOrderId, Long spuId, Long skuId, String skuSnapshot) {
    OrderItem item = new OrderItem();
    item.setId(id);
    item.setSubOrderId(subOrderId);
    item.setSpuId(spuId);
    item.setSkuId(skuId);
    item.setSkuCode("SKU-" + skuId);
    item.setSkuName("Item-" + skuId);
    item.setSkuSnapshot(skuSnapshot);
    item.setQuantity(2);
    item.setUnitPrice(BigDecimal.valueOf(40));
    item.setTotalPrice(BigDecimal.valueOf(80));
    return item;
  }

  private JwtAuthenticationToken authentication(
      String userId, String primaryRole, String... authorities) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("user_id", userId)
            .claim("username", "tester")
            .build();
    return new JwtAuthenticationToken(
        jwt, AuthorityUtils.createAuthorityList(merge(primaryRole, authorities)));
  }

  private String[] merge(String first, String[] remaining) {
    String[] merged = new String[remaining.length + 1];
    merged[0] = first;
    System.arraycopy(remaining, 0, merged, 1, remaining.length);
    return merged;
  }
}
