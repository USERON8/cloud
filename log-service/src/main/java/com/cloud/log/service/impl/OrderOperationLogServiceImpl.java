package com.cloud.log.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.log.module.entity.OrderOperationLog;
import com.cloud.log.service.OrderOperationLogService;
import com.cloud.log.mapper.OrderOperationLogMapper;
import org.springframework.stereotype.Service;

/**
* @author what's up
* @description 针对表【order_operation_log(订单操作日志)】的数据库操作Service实现
* @createDate 2025-08-20 19:16:38
*/
@Service
public class OrderOperationLogServiceImpl extends ServiceImpl<OrderOperationLogMapper, OrderOperationLog>
    implements OrderOperationLogService{

}
