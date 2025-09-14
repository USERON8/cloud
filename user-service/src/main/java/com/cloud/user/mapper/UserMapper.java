package com.cloud.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.user.module.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author what's up
 * @description 针对表【users(用户表)】的数据库操作Mapper
 * @createDate 2025-09-06 19:31:12
 * @Entity com.cloud.user.module.entity.User
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 移除原生SQL查询，在Service层使用Lambda表达式实现
    // 这样可以避免SQL注入风险，并且类型更安全

    // 移除不安全的原生SQL更新，使用MyBatis-Plus的逻辑删除
    // MyBatis-Plus会自动处理逻辑删除字段
}




