package com.cloud.api.user;

import com.cloud.common.domain.dto.user.UserProfileDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;

/**
 * 用户基础资料内部调用契约。
 *
 * <p>认证、商品、订单等服务通过该接口读取用户资料和商户归属关系。
 */
public interface UserDubboApi {

  /** 按用户 ID 查询用户资料。 */
  UserProfileDTO findById(Long id);

  /** 判断用户是否为指定商户的归属人。 */
  Boolean isMerchantOwner(Long merchantId, Long userId);

  /** 按用户 ID 查询其拥有的商户 ID。 */
  Long findMerchantIdByOwnerUserId(Long userId);

  /** 创建用户资料。 */
  Long create(UserProfileUpsertDTO profileUpsertDTO);

  /** 更新用户资料。 */
  Boolean update(UserProfileUpsertDTO profileUpsertDTO);
}
