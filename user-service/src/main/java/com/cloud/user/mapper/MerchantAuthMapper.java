package com.cloud.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.user.module.entity.MerchantAuth;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author what's up
 * @description 针对表【merchant_auth(商家认证表)】的数据库操作Mapper
 * @createDate 2025-09-06 19:31:12
 * @Entity com.cloud.user.module.entity.MerchantAuth
 */
@Mapper
public interface MerchantAuthMapper extends BaseMapper<MerchantAuth> {

}




