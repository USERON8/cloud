package com.cloud.common.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;






@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    


    private Long current;

    


    private Long size;

    


    private Long total;

    


    private Long pages;

    


    private List<T> records;

    


    private Boolean hasPrevious;

    


    private Boolean hasNext;

    public PageResult(Long current, Long size, Long total, List<T> records) {
        this.current = current;
        this.size = size;
        this.total = total;
        this.records = records;
        this.pages = (total + size - 1) / size; 
        this.hasPrevious = current > 1;
        this.hasNext = current < pages;
    }

    


    public static <T> PageResult<T> of(Long current, Long size, Long total, List<T> records) {
        return new PageResult<>(current, size, total, records);
    }

    


    public static <T> PageResult<T> of(List<T> records, Long total, Long current, Long size) {
        return new PageResult<>(current, size, total, records);
    }

    


    public static <T> PageResult<T> empty(Long current, Long size) {
        return new PageResult<>(current, size, 0L, List.of());
    }
}
