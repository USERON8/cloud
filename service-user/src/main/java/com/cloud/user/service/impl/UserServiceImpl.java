package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserService;
import org.springframework.stereotype.Service;

/**
 * @author what's up
 * @description 针对表【users(用户表)】的数据库操作Service实现
 * @createDate 2025-08-09 13:41:14
 */
@Service
public class UserServiceImpl extends CrudRepository<UserMapper, User>
        implements UserService {

}




