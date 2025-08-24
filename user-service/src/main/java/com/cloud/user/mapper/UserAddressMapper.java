package com.cloud.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.user.module.entity.UserAddress;
import org.apache.ibatis.annotations.Update;

/**
 * @author what's up
 * @description 针对表【user_address(用户地址表)】的数据库操作Mapper
 * @createDate 2025-08-20 12:35:31
 * @Entity com.cloud.user.module.entity.UserAddress
 */
public interface UserAddressMapper extends BaseMapper<UserAddress> {
    @Update("update user_address set deleted = 1 where id = #{id}")
    void updateDeleted(Long id);
}




