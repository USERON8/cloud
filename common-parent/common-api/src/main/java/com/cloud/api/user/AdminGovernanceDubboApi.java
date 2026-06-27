package com.cloud.api.user;

import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.common.domain.vo.user.AdminPageVO;

/**
 * 管理员治理内部调用契约。
 *
 * <p>治理服务通过该接口代理管理员分页、状态调整和密码重置等后台运维能力。
 */
public interface AdminGovernanceDubboApi {

  /** 分页查询管理员。 */
  AdminPageVO getAdminsPage(Integer page, Integer size);

  /** 查询管理员详情。 */
  AdminDTO getAdminById(Long id);

  /** 创建管理员。 */
  AdminDTO createAdmin(AdminUpsertRequestDTO requestDTO);

  /** 更新管理员。 */
  Boolean updateAdmin(Long id, AdminUpsertRequestDTO requestDTO);

  /** 删除管理员。 */
  Boolean deleteAdmin(Long id);

  /** 更新管理员状态。 */
  Boolean updateAdminStatus(Long id, Integer status);

  /** 重置管理员密码并返回新密码或提示信息。 */
  String resetPassword(Long id);
}
