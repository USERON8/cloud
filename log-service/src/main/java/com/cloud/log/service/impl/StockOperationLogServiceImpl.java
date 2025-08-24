package com.cloud.log.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.log.module.entity.StockOperationLog;
import com.cloud.log.service.StockOperationLogService;
import com.cloud.log.mapper.StockOperationLogMapper;
import org.springframework.stereotype.Service;

/**
* @author what's up
* @description 针对表【stock_operation_log(库存变更审计表)】的数据库操作Service实现
* @createDate 2025-08-20 19:16:38
*/
@Service
public class StockOperationLogServiceImpl extends ServiceImpl<StockOperationLogMapper, StockOperationLog>
    implements StockOperationLogService{

}
