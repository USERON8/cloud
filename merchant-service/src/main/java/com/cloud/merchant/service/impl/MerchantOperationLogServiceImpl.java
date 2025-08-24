package com.cloud.merchant.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.merchant.module.entity.MerchantOperationLog;
import com.cloud.merchant.service.MerchantOperationLogService;
import com.cloud.merchant.mapper.MerchantOperationLogMapper;
import org.springframework.stereotype.Service;

/**
* @author what's up
* @description 针对表【merchant_operation_log(商家操作日志表)】的数据库操作Service实现
* @createDate 2025-08-17 20:55:29
*/
@Service
public class MerchantOperationLogServiceImpl extends ServiceImpl<MerchantOperationLogMapper, MerchantOperationLog>
    implements MerchantOperationLogService{

}




