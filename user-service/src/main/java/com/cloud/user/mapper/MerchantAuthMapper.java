package com.cloud.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.user.module.entity.MerchantAuth;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家认证Mapper接口
 */
@Mapper
public interface MerchantAuthMapper extends BaseMapper<MerchantAuth> {
}