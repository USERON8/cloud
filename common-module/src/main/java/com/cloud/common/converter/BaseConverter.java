package com.cloud.common.converter;

import java.util.List;













public interface BaseConverter<E, D, V> {

    





    D toDTO(E entity);

    





    E toEntity(D dto);

    





    List<D> toDTOList(List<E> entities);

    





    List<E> toEntityList(List<D> dtos);

    





    V toVO(E entity);

    





    V dtoToVO(D dto);

    





    List<V> toVOList(List<E> entities);

    





    List<V> dtoToVOList(List<D> dtos);
}
