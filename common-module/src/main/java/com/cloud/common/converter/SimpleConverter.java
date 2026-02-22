package com.cloud.common.converter;

import java.util.List;












public interface SimpleConverter<E, D> {

    





    D toDTO(E entity);

    





    E toEntity(D dto);

    





    List<D> toDTOList(List<E> entities);

    





    List<E> toEntityList(List<D> dtos);
}
