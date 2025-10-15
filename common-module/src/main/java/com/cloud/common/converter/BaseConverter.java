package com.cloud.common.converter;

import java.util.List;

/**
 * 基础转换器接口
 * 定义通用的实体-DTO-VO转换方法
 * 所有具体的Converter接口都应该继承此接口
 *
 * @param <E> Entity - 实体类型
 * @param <D> DTO - 数据传输对象类型
 * @param <V> VO - 视图对象类型
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
public interface BaseConverter<E, D, V> {

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

    /**
     * 实体转VO
     *
     * @param entity 实体对象
     * @return VO对象
     */
    V toVO(E entity);

    /**
     * DTO转VO
     *
     * @param dto DTO对象
     * @return VO对象
     */
    V dtoToVO(D dto);

    /**
     * 实体列表转VO列表
     *
     * @param entities 实体列表
     * @return VO列表
     */
    List<V> toVOList(List<E> entities);

    /**
     * DTO列表转VO列表
     *
     * @param dtos DTO列表
     * @return VO列表
     */
    List<V> dtoToVOList(List<D> dtos);
}
