package com.cloud.log.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.log.module.entity.AdminOperationLog;
import com.cloud.log.service.AdminOperationLogService;
import com.cloud.log.mapper.AdminOperationLogMapper;
import org.springframework.stereotype.Service;

/**
* @author what's up
* @description 针对表【admin_operation_log(管理员操作日志表)】的数据库操作Service实现
* @createDate 2025-08-20 19:16:38
*/
@Service
public class AdminOperationLogServiceImpl extends ServiceImpl<AdminOperationLogMapper, AdminOperationLog>
    implements AdminOperationLogService{

}




