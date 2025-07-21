package converter;

import java.util.List;

/**
 * 通用转换器基类
 */
public interface BaseConverter<E, V> {
    
    /**
     * 实体转VO
     */
    V toVO(E entity);
    
    /**
     * 实体列表转VO列表
     */
    List<V> toVOList(List<E> entityList);
    
    /**
     * VO转实体
     */
    E toEntity(V vo);
    
    /**
     * VO列表转实体列表
     */
    List<E> toEntityList(List<V> voList);
}