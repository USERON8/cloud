package com.cloud.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.user.module.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author what's up
 * @description 针对表【users(用户表)】的数据库操作Mapper
 * @createDate 2025-08-09 13:41:14
 * @Entity com.cloud.user.module.entity.User
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    void delByAvatarFileNameAndAvatarUrl(String avatarFileName, String avatarUrl);

}




