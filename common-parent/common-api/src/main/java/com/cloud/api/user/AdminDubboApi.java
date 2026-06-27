package com.cloud.api.user;

import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import java.util.List;

/**
 * 管理员基础资料内部调用契约。
 *
 * <p>用于认证、治理和后台管理链路读取或维护管理员资料。
 */
public interface AdminDubboApi {

  /** 按管理员 ID 查询详情。 */
  AdminDTO findById(Long id);

  /** 查询全部管理员。 */
  List<AdminDTO> findAll();

  /** 创建管理员。 */
  Long create(AdminUpsertRequestDTO requestDTO);

  /** 更新管理员资料。 */
  Boolean update(Long id, AdminUpsertRequestDTO requestDTO);

  /** 删除管理员。 */
  Boolean delete(Long id);
}
