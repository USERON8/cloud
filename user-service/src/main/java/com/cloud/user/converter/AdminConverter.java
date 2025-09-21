package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.vo.AdminVO;
import com.cloud.user.module.entity.Admin;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 管理员转换器
 *
 * @author what's up
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // 忽略未映射目标属性
        unmappedSourcePolicy = ReportingPolicy.IGNORE  // 忽略未映射源属性
)
public interface AdminConverter {

    /**
     * 转换管理员实体为DTO
     *
     * @param admin 管理员实体
     * @return 管理员DTO
     */
    AdminDTO toDTO(Admin admin);

    /**
     * 转换管理员DTO为实体
     *
     * @param adminDTO 管理员DTO
     * @return 管理员实体
     */
    Admin toEntity(AdminDTO adminDTO);

    /**
     * 转换管理员实体为VO
     *
     * @param admin 管理员实体
     * @return 管理员VO
     */
    AdminVO toVO(Admin admin);

    /**
     * 转换管理员实体列表为VO列表
     *
     * @param admins 管理员实体列表
     * @return 管理员VO列表
     */
    List<AdminVO> toVOList(List<Admin> admins);

    List<AdminDTO> toDTOList(List<Admin> admins);
}