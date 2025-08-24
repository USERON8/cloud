package com.cloud.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.admin.module.entity.Admin;
import com.cloud.admin.service.AdminService;
import com.cloud.admin.mapper.AdminMapper;
import org.springframework.stereotype.Service;

/**
* @author what's up
* @description 针对表【admin(管理员信息表)】的数据库操作Service实现
* @createDate 2025-08-17 20:57:18
*/
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin>
    implements AdminService{

}




