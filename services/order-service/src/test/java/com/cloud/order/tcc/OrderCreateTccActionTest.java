package com.cloud.order.tcc;

import com.cloud.order.entity.OrderTccLog;
import com.cloud.order.mapper.CartItemMapper;
import com.cloud.order.mapper.CartMapper;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.mapper.OrderTccLogMapper;
import com.cloud.order.messaging.OrderTimeoutMessageProducer;
import com.cloud.order.service.support.OrderAggregateCacheService;
import com.cloud.api.product.ProductDubboApi;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreateTccActionTest {

    @Mock
    private OrderMainMapper orderMainMapper;

    @Mock
    private OrderSubMapper orderSubMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private OrderTccLogMapper orderTccLogMapper;

    @Mock
    private OrderAggregateCacheService orderAggregateCacheService;

    @Mock
    private OrderTimeoutMessageProducer orderTimeoutMessageProducer;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private ProductDubboApi productDubboApi;

    private OrderCreateTccAction action;

    @BeforeEach
    void setUp() {
        action = new OrderCreateTccAction(
                orderMainMapper,
                orderSubMapper,
                orderItemMapper,
                orderTccLogMapper,
                orderAggregateCacheService,
                orderTimeoutMessageProducer,
                cartMapper,
                cartItemMapper
        );
        injectProductDubboApi(action, productDubboApi);
    }

    @Test
    void rollbackShouldInsertCancelLogWhenNoTryRecord() {
        BusinessActionContext context = org.mockito.Mockito.mock(BusinessActionContext.class);
        when(context.getActionContext("idempotencyKey")).thenReturn("idem-1");
        when(orderTccLogMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        boolean result = action.rollback(context);

        assertThat(result).isTrue();
        ArgumentCaptor<OrderTccLog> captor = ArgumentCaptor.forClass(OrderTccLog.class);
        verify(orderTccLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getBusinessKey()).isEqualTo("idem-1");
        assertThat(captor.getValue().getStatus()).isEqualTo("CANCEL");
    }

    private void injectProductDubboApi(OrderCreateTccAction target, ProductDubboApi api) {
        try {
            java.lang.reflect.Field field = OrderCreateTccAction.class.getDeclaredField("productDubboApi");
            field.setAccessible(true);
            field.set(target, api);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to inject productDubboApi", ex);
        }
    }
}
