package com.cloud.order.service.impl;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.dto.CreateMainOrderRequest;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderPlacementService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.StockReservationRemoteService;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPlacementServiceImplTest {

    @Mock
    private OrderMainMapper orderMainMapper;

    @Mock
    private OrderSubMapper orderSubMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private StockReservationRemoteService stockReservationRemoteService;

    @Mock
    private OrderService orderService;

    @Mock
    private TransactionOperations transactionOperations;

    @Mock
    private Executor orderStockReservationExecutor;

    private OrderPlacementServiceImpl orderPlacementService;

    @BeforeEach
    void setUp() {
        orderPlacementService = createService(orderStockReservationExecutor);
        lenient().when(transactionOperations.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            TransactionCallback<Object> callback = (TransactionCallback<Object>) invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        lenient().doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(orderStockReservationExecutor).execute(any(Runnable.class));
    }

    @Test
    void createOrderShouldReserveStockAndPersistReservedStatuses() {
        CreateMainOrderRequest request = createRequest("idem-1",
                subOrder(30001L, itemRequest(101L, 1), itemRequest(101L, 2), itemRequest(102L, 1)));

        when(orderMainMapper.selectActiveByIdempotencyKey("idem-1")).thenReturn(null);
        mockGeneratedIds();
        when(stockReservationRemoteService.reserve(any())).thenReturn(true);

        OrderAggregateResponse result = orderPlacementService.createOrder(request);

        assertThat(result.getMainOrder().getId()).isEqualTo(10L);
        assertThat(result.getMainOrder().getOrderStatus()).isEqualTo("STOCK_RESERVED");
        assertThat(result.getSubOrders())
                .singleElement()
                .extracting(OrderAggregateResponse.SubOrderWithItems::getSubOrder)
                .extracting(OrderSub::getOrderStatus)
                .isEqualTo("STOCK_RESERVED");

        ArgumentCaptor<StockOperateCommandDTO> captor = ArgumentCaptor.forClass(StockOperateCommandDTO.class);
        verify(stockReservationRemoteService, org.mockito.Mockito.times(2)).reserve(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(StockOperateCommandDTO::getSubOrderNo, StockOperateCommandDTO::getSkuId, StockOperateCommandDTO::getQuantity)
                .containsExactlyInAnyOrder(
                        Tuple.tuple(result.getSubOrders().get(0).getSubOrder().getSubOrderNo(), 101L, 3),
                        Tuple.tuple(result.getSubOrders().get(0).getSubOrder().getSubOrderNo(), 102L, 1)
                );
        verify(orderSubMapper).updateById(any(OrderSub.class));
        verify(orderMainMapper).updateById(any(OrderMain.class));
        verify(orderService, never()).getOrderAggregate(any());
    }

    @Test
    void createOrderShouldFailWhenStockReservationFails() {
        CreateMainOrderRequest request = createRequest("idem-2",
                subOrder(30001L, itemRequest(301L, 1)));

        when(orderMainMapper.selectActiveByIdempotencyKey("idem-2")).thenReturn(null);
        mockGeneratedIds();
        when(stockReservationRemoteService.reserve(any())).thenReturn(false);

        assertThatThrownBy(() -> orderPlacementService.createOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reserve stock failed");
        verify(orderSubMapper, never()).updateById(any(OrderSub.class));
        verify(orderMainMapper, never()).updateById(any(OrderMain.class));
    }

    @Test
    void createOrderShouldLoadExistingAggregateWhenIdempotencyKeyExists() {
        CreateMainOrderRequest request = createRequest("idem-3",
                subOrder(30001L, itemRequest(401L, 1)));

        OrderMain existing = new OrderMain();
        existing.setId(30L);

        OrderAggregateResponse aggregate = aggregate(
                "M300",
                wrappedSubOrder(31L, "S301", "STOCK_RESERVED", item(401L, 1))
        );

        when(orderMainMapper.selectActiveByIdempotencyKey("idem-3")).thenReturn(existing);
        when(orderService.getOrderAggregate(30L)).thenReturn(aggregate);

        OrderAggregateResponse result = orderPlacementService.createOrder(request);

        assertThat(result).isSameAs(aggregate);
        verify(orderMainMapper, never()).insert(any(OrderMain.class));
        verify(stockReservationRemoteService, never()).reserve(any());
    }

    @Test
    void createOrderShouldReserveDistinctSkusInParallel() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            OrderPlacementService service = createService(executor);
            CreateMainOrderRequest request = createRequest("idem-4",
                    subOrder(30001L, itemRequest(501L, 1), itemRequest(502L, 1)));

            when(orderMainMapper.selectActiveByIdempotencyKey("idem-4")).thenReturn(null);
            mockGeneratedIds();

            CountDownLatch started = new CountDownLatch(2);
            CountDownLatch release = new CountDownLatch(1);
            AtomicInteger active = new AtomicInteger();
            AtomicInteger maxActive = new AtomicInteger();
            when(stockReservationRemoteService.reserve(any())).thenAnswer(invocation -> {
                int current = active.incrementAndGet();
                maxActive.accumulateAndGet(current, Math::max);
                started.countDown();
                try {
                    if (!release.await(2, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("reserve tasks did not run concurrently");
                    }
                } finally {
                    active.decrementAndGet();
                }
                return true;
            });

            CompletableFuture<OrderAggregateResponse> future = CompletableFuture.supplyAsync(() -> service.createOrder(request));
            assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
            release.countDown();

            OrderAggregateResponse result = future.get(2, TimeUnit.SECONDS);
            assertThat(result.getMainOrder().getOrderStatus()).isEqualTo("STOCK_RESERVED");
            assertThat(maxActive.get()).isGreaterThan(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void createOrderShouldKeepSameSkuReservationsSequential() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            OrderPlacementService service = createService(executor);
            CreateMainOrderRequest request = createRequest("idem-5",
                    subOrder(30001L, itemRequest(601L, 1)),
                    subOrder(30002L, itemRequest(601L, 1)));

            when(orderMainMapper.selectActiveByIdempotencyKey("idem-5")).thenReturn(null);
            mockGeneratedIds();

            CountDownLatch firstStarted = new CountDownLatch(1);
            CountDownLatch releaseFirst = new CountDownLatch(1);
            CountDownLatch secondStarted = new CountDownLatch(1);
            AtomicInteger invocation = new AtomicInteger();
            when(stockReservationRemoteService.reserve(any())).thenAnswer(invocationOnMock -> {
                int current = invocation.getAndIncrement();
                if (current == 0) {
                    firstStarted.countDown();
                    if (!releaseFirst.await(2, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("first reserve was not released");
                    }
                } else {
                    secondStarted.countDown();
                }
                return true;
            });

            CompletableFuture<OrderAggregateResponse> future = CompletableFuture.supplyAsync(() -> service.createOrder(request));
            assertThat(firstStarted.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(secondStarted.await(250, TimeUnit.MILLISECONDS)).isFalse();
            releaseFirst.countDown();

            OrderAggregateResponse result = future.get(2, TimeUnit.SECONDS);
            assertThat(result.getMainOrder().getOrderStatus()).isEqualTo("STOCK_RESERVED");
            assertThat(secondStarted.await(1, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    private OrderPlacementServiceImpl createService(Executor executor) {
        return new OrderPlacementServiceImpl(
                orderMainMapper,
                orderSubMapper,
                orderItemMapper,
                orderService,
                stockReservationRemoteService,
                transactionOperations,
                executor
        );
    }

    private void mockGeneratedIds() {
        AtomicLong subOrderIds = new AtomicLong(11L);
        AtomicLong itemIds = new AtomicLong(101L);

        doAnswer(invocation -> {
            OrderMain mainOrder = invocation.getArgument(0);
            mainOrder.setId(10L);
            return 1;
        }).when(orderMainMapper).insert(any(OrderMain.class));

        doAnswer(invocation -> {
            OrderSub subOrder = invocation.getArgument(0);
            subOrder.setId(subOrderIds.getAndIncrement());
            return 1;
        }).when(orderSubMapper).insert(any(OrderSub.class));

        doAnswer(invocation -> {
            OrderItem orderItem = invocation.getArgument(0);
            orderItem.setId(itemIds.getAndIncrement());
            return 1;
        }).when(orderItemMapper).insert(any(OrderItem.class));
    }

    private CreateMainOrderRequest createRequest(String idempotencyKey,
                                                 CreateMainOrderRequest.CreateSubOrderRequest... subOrders) {
        CreateMainOrderRequest request = new CreateMainOrderRequest();
        request.setIdempotencyKey(idempotencyKey);
        request.setUserId(999999L);
        request.setTotalAmount(new BigDecimal("99.99"));
        request.setPayableAmount(new BigDecimal("99.99"));
        request.setRemark("test");
        request.setSubOrders(List.of(subOrders));
        return request;
    }

    private CreateMainOrderRequest.CreateSubOrderRequest subOrder(Long merchantId,
                                                                  CreateMainOrderRequest.CreateOrderItemRequest... items) {
        CreateMainOrderRequest.CreateSubOrderRequest subOrder = new CreateMainOrderRequest.CreateSubOrderRequest();
        subOrder.setMerchantId(merchantId);
        subOrder.setItemAmount(new BigDecimal("99.99"));
        subOrder.setShippingFee(BigDecimal.ZERO);
        subOrder.setDiscountAmount(BigDecimal.ZERO);
        subOrder.setPayableAmount(new BigDecimal("99.99"));
        subOrder.setReceiverName("tester");
        subOrder.setReceiverPhone("13800138000");
        subOrder.setReceiverAddress("test road");
        subOrder.setItems(List.of(items));
        return subOrder;
    }

    private CreateMainOrderRequest.CreateOrderItemRequest itemRequest(Long skuId, Integer quantity) {
        CreateMainOrderRequest.CreateOrderItemRequest item = new CreateMainOrderRequest.CreateOrderItemRequest();
        item.setSpuId(skuId + 1000);
        item.setSkuId(skuId);
        item.setSkuCode("SKU-" + skuId);
        item.setSkuName("sku-" + skuId);
        item.setSkuSnapshot("{}");
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal("99.99"));
        item.setTotalPrice(new BigDecimal("99.99"));
        return item;
    }

    private OrderAggregateResponse aggregate(String mainOrderNo, OrderAggregateResponse.SubOrderWithItems... subOrders) {
        OrderMain mainOrder = new OrderMain();
        mainOrder.setId(1L);
        mainOrder.setMainOrderNo(mainOrderNo);

        OrderAggregateResponse response = new OrderAggregateResponse();
        response.setMainOrder(mainOrder);
        response.setSubOrders(List.of(subOrders));
        return response;
    }

    private OrderAggregateResponse.SubOrderWithItems wrappedSubOrder(Long subOrderId, String subOrderNo, String status, OrderItem... items) {
        OrderSub subOrder = new OrderSub();
        subOrder.setId(subOrderId);
        subOrder.setSubOrderNo(subOrderNo);
        subOrder.setOrderStatus(status);

        OrderAggregateResponse.SubOrderWithItems wrapped = new OrderAggregateResponse.SubOrderWithItems();
        wrapped.setSubOrder(subOrder);
        wrapped.setItems(List.of(items));
        return wrapped;
    }

    private OrderItem item(Long skuId, Integer quantity) {
        OrderItem item = new OrderItem();
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        return item;
    }
}
