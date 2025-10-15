package com.cloud.common.converter;

import java.util.List;

/**
 * 简化版基础转换器接口
 * 仅包含实体-DTO转换，不包含VO
 * 适用于不需要VO层的简单场景
 *
 * @param <E> Entity - 实体类型
 * @param <D> DTO - 数据传输对象类型
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
public interface SimpleConverter<E, D> {

    /**
     * 实体转DTO
     *
     * @param entity 实体对象
     * @return DTO对象
     */
    D toDTO(E entity);

    /**
     * DTO转实体
     *
     * @param dto DTO对象
     * @return 实体对象
     */
    E toEntity(D dto);

    /**
     * 实体列表转DTO列表
     *
     * @param entities 实体列表
     * @return DTO列表
     */
    List<D> toDTOList(List<E> entities);

    /**
     * DTO列表转实体列表
     *
     * @param dtos DTO列表
     * @return 实体列表
     */
    List<E> toEntityList(List<D> dtos);
}
