package com.cloud.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.PageResult;
import com.cloud.order.converter.AfterSaleDtoConverter;
import com.cloud.order.dto.AfterSaleDTO;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.dto.OrderSummaryDTO;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.service.OrderBatchService;
import com.cloud.order.service.OrderPlacementService;
import com.cloud.order.service.OrderQueryService;
import com.cloud.order.service.OrderService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

  @Mock private OrderService orderService;

  @Mock private OrderPlacementService orderPlacementService;

  @Mock private OrderBatchService orderBatchService;

  @Mock private OrderQueryService orderQueryService;

  @Mock private AfterSaleDtoConverter afterSaleDtoConverter;

  @InjectMocks private OrderController orderController;

  @Test
  void createMainOrderShouldFillUserIdFromTokenForRegularUser() {
    CreateMainOrderRequest request = buildDirectOrderRequest();
    request.setUserId(null);
    OrderAggregateResponse response = new OrderAggregateResponse();
    when(orderPlacementService.createOrder(same(request))).thenReturn(response);

    var result =
        orderController.createMainOrder(
            request, " key-123 ", authentication("101", "ROLE_USER", "order:create"));

    assertThat(request.getUserId()).isEqualTo(101L);
    assertThat(request.getIdempotencyKey()).isEqualTo("key-123");
    assertThat(result.getData()).isSameAs(response);
    verify(orderPlacementService).createOrder(same(request));
  }

  @Test
  void createMainOrderShouldRejectMissingUserIdForAdmin() {
    CreateMainOrderRequest request = buildDirectOrderRequest();
    request.setUserId(null);

    assertThatThrownBy(
            () ->
                orderController.createMainOrder(
                    request, "admin-key", authentication("1", "ROLE_ADMIN", "order:create")))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex -> {
              BizException bizException = (BizException) ex;
              assertThat(bizException.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
            })
        .hasMessageContaining("userId is required for admin order creation");
  }

  @Test
  void shipOrderShouldRejectRegularUser() {
    assertThatThrownBy(
            () ->
                orderController.shipOrderStandard(
                    88L, "SF", "T-88", authentication("101", "ROLE_USER", "order:query")))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex -> {
              BizException bizException = (BizException) ex;
              assertThat(bizException.getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode());
            })
        .hasMessageContaining("shipping requires merchant or admin privileges");
  }

  @Test
  void shipOrderShouldRejectMissingShippingCompany() {
    assertThatThrownBy(
            () ->
                orderController.shipOrderStandard(
                    88L, " ", "T-88", authentication("201", "ROLE_MERCHANT", "order:query")))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex -> {
              BizException bizException = (BizException) ex;
              assertThat(bizException.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
            })
        .hasMessageContaining("shipping company is required");
  }

  @Test
  void batchShipShouldTrimShippingFieldsForMerchant() {
    when(orderBatchService.batchApply(
            eq(java.util.List.of(7L)),
            org.mockito.ArgumentMatchers.any(),
            eq(OrderAction.SHIP),
            eq("SF"),
            eq("TRACK-7"),
            eq(null)))
        .thenReturn(1);

    var result =
        orderController.batchShip(
            java.util.List.of(7L),
            " SF ",
            " TRACK-7 ",
            authentication("201", "ROLE_MERCHANT", "order:query"));

    assertThat(result.getData()).isEqualTo(1);
    verify(orderBatchService)
        .batchApply(
            eq(java.util.List.of(7L)),
            org.mockito.ArgumentMatchers.any(),
            eq(OrderAction.SHIP),
            eq("SF"),
            eq("TRACK-7"),
            eq(null));
  }

  @Test
  void applyAfterSaleShouldFillUserIdFromTokenForRegularUser() {
    AfterSaleDTO dto = new AfterSaleDTO();
    dto.setMainOrderId(10L);
    dto.setSubOrderId(11L);
    dto.setApplyAmount(BigDecimal.valueOf(30));

    AfterSale mapped = new AfterSale();
    mapped.setMainOrderId(10L);
    mapped.setSubOrderId(11L);
    mapped.setApplyAmount(BigDecimal.valueOf(30));
    AfterSale created = new AfterSale();
    AfterSaleDTO responseDto = new AfterSaleDTO();

    when(afterSaleDtoConverter.toEntity(same(dto))).thenReturn(mapped);
    when(orderService.applyAfterSale(same(mapped))).thenReturn(created);
    when(afterSaleDtoConverter.toDto(same(created))).thenReturn(responseDto);

    var result =
        orderController.applyAfterSale(dto, authentication("101", "ROLE_USER", "order:refund"));

    assertThat(mapped.getUserId()).isEqualTo(101L);
    assertThat(result.getData()).isSameAs(responseDto);
    verify(orderService).applyAfterSale(same(mapped));
  }

  @Test
  void listOrdersShouldPreferMerchantIdAndAllowLegacyShopIdAlias() {
    PageResult<OrderSummaryDTO> pageResult = PageResult.of(1L, 20L, 0L, java.util.List.of());
    when(orderQueryService.listOrders(
            org.mockito.ArgumentMatchers.any(), eq(1), eq(20), eq(101L), eq(201L), eq(2)))
        .thenReturn(pageResult);

    var result =
        orderController.listOrders(
            1, 20, 101L, 201L, null, 2, authentication("1", "ROLE_ADMIN", "order:query"));

    assertThat(result.getData()).isSameAs(pageResult);
    verify(orderQueryService)
        .listOrders(org.mockito.ArgumentMatchers.any(), eq(1), eq(20), eq(101L), eq(201L), eq(2));
  }

  @Test
  void listOrdersShouldRejectConflictingMerchantIdAndShopIdAlias() {
    assertThatThrownBy(
            () ->
                orderController.listOrders(
                    1,
                    20,
                    null,
                    301L,
                    401L,
                    null,
                    authentication("1", "ROLE_ADMIN", "order:query")))
        .isInstanceOf(BizException.class)
        .satisfies(
            ex -> {
              BizException bizException = (BizException) ex;
              assertThat(bizException.getCode()).isEqualTo(ResultCode.BAD_REQUEST.getCode());
            })
        .hasMessageContaining("merchantId and shopId must match");
  }

  @Test
  void advanceAfterSaleStatusShouldAllowMerchantProcessAction() {
    AfterSale afterSale = new AfterSale();
    afterSale.setId(55L);
    afterSale.setMerchantId(201L);
    afterSale.setUserId(101L);
    afterSale.setStatus("RECEIVED");

    AfterSale updated = new AfterSale();
    updated.setId(55L);
    updated.setStatus("REFUNDING");
    AfterSaleDTO response = new AfterSaleDTO();
    response.setId(55L);
    response.setStatus("REFUNDING");

    when(orderService.getAfterSale(55L)).thenReturn(afterSale);
    when(orderService.advanceAfterSaleStatus(
            55L, com.cloud.order.enums.AfterSaleAction.PROCESS, "start refund"))
        .thenReturn(updated);
    when(afterSaleDtoConverter.toDto(updated)).thenReturn(response);

    var result =
        orderController.advanceAfterSaleStatus(
            55L, "PROCESS", "start refund", authentication("201", "ROLE_MERCHANT", "order:refund"));

    assertThat(result.getData()).isSameAs(response);
    verify(orderService)
        .advanceAfterSaleStatus(55L, com.cloud.order.enums.AfterSaleAction.PROCESS, "start refund");
  }

  private CreateMainOrderRequest buildDirectOrderRequest() {
    CreateMainOrderRequest request = new CreateMainOrderRequest();
    request.setSpuId(10L);
    request.setSkuId(20L);
    request.setQuantity(1);
    request.setTotalAmount(BigDecimal.valueOf(99.99));
    request.setPayableAmount(BigDecimal.valueOf(99.99));
    request.setReceiverName("tester");
    request.setReceiverPhone("13800138000");
    request.setReceiverAddress("cloud road");
    return request;
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
