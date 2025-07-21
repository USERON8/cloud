package utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import domain.PageQuery;
import domain.PageResult;

import java.util.List;
import java.util.function.Function;

/**
 * 分页工具类
 */
public class PageUtils {
    
    /**
     * 创建MyBatis-Plus分页对象
     */
    public static <T> Page<T> buildPage(PageQuery pageQuery) {
        return new Page<>(pageQuery.getCurrent(), pageQuery.getSize());
    }
    
    /**
     * IPage转换为PageResult
     */
    public static <T> PageResult<T> toPageResult(IPage<T> page) {
        return PageResult.of(
            page.getCurrent(),
            page.getSize(),
            page.getTotal(),
            page.getRecords()
        );
    }
    
    /**
     * IPage转换为PageResult，并转换数据类型
     */
    public static <T, R> PageResult<R> toPageResult(IPage<T> page, Function<T, R> converter) {
        List<R> records = page.getRecords().stream()
                .map(converter)
                .toList();
        
        return PageResult.of(
            page.getCurrent(),
            page.getSize(),
            page.getTotal(),
            records
        );
    }
    
    /**
     * 创建空的分页结果
     */
    public static <T> PageResult<T> emptyPage(PageQuery pageQuery) {
        return PageResult.empty((long) pageQuery.getCurrent(), (long) pageQuery.getSize());
    }
}