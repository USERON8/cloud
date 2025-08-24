package com.cloud.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.order.module.entity.Order;
import com.cloud.order.service.OrderService;
import com.cloud.order.mapper.OrderMapper;
import org.springframework.stereotype.Service;

/**
* @author what's up
* @description 针对表【order(订单主表)】的数据库操作Service实现
* @createDate 2025-08-16 19:50:33
*/
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>
    implements OrderService{

}




