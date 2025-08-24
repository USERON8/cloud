package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.order.module.entity.OrderItem;
import com.cloud.order.service.OrderItemService;
import com.cloud.order.mapper.OrderItemMapper;
import org.springframework.stereotype.Service;

/**
* @author what's up
* @description 针对表【order_item(订单明细表)】的数据库操作Service实现
* @createDate 2025-08-16 19:50:33
*/
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem>
    implements OrderItemService{

}




