package com.cloud.log.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.log.module.entity.MerchantOperationLog;
import com.cloud.log.service.MerchantOperationLogService;
import com.cloud.log.mapper.MerchantOperationLogMapper;
import org.springframework.stereotype.Service;

/**
* @author what's up
* @description 针对表【merchant_operation_log(商家操作日志表)】的数据库操作Service实现
* @createDate 2025-08-20 19:16:38
*/
@Service
public class MerchantOperationLogServiceImpl extends ServiceImpl<MerchantOperationLogMapper, MerchantOperationLog>
    implements MerchantOperationLogService{

}
