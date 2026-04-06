package com.cloud.order.service;

import com.cloud.order.dto.CartDTO;
import com.cloud.order.dto.CartSyncRequest;

public interface CartService {

  CartDTO getCurrentCart(Long userId);

  CartDTO syncCart(Long userId, CartSyncRequest request);
}
