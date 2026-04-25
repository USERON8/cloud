package com.cloud.order.service.support;

import static org.mockito.Mockito.verifyNoInteractions;

import com.cloud.order.mapper.CartItemMapper;
import com.cloud.order.mapper.CartMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderPlacementSupportTest {

  @Mock private CartMapper cartMapper;
  @Mock private CartItemMapper cartItemMapper;
  @Mock private com.cloud.common.remote.RemoteCallSupport remoteCallSupport;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private OrderPlacementSupport orderPlacementSupport;

  @Test
  void markCartCheckedOutUsesDedicatedIndexedUpdates() {
    orderPlacementSupport.markCartCheckedOut(11L, 22L);

    InOrder inOrder = Mockito.inOrder(cartItemMapper, cartMapper);
    inOrder.verify(cartItemMapper).deletePhysicalByCartIdAndUserId(11L, 22L);
    inOrder.verify(cartMapper).deleteCheckedOutByUserId(22L);
    inOrder.verify(cartMapper).markCheckedOutByIdAndUserId(11L, 22L);
  }

  @Test
  void markCartCheckedOutSkipsNullCartId() {
    orderPlacementSupport.markCartCheckedOut(null, 22L);

    verifyNoInteractions(cartItemMapper, cartMapper);
  }
}
