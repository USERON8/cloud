package com.cloud.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;








@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "鎼滅储缁撴灉鍝嶅簲")
public class SearchResult<T> {

    


    @Schema(description = "鎼滅储缁撴灉鍒楄〃")
    private List<T> list;

    


    @Schema(description = "鎬昏褰曟暟", example = "1000")
    private Long total;

    


    @Schema(description = "褰撳墠椤电爜", example = "0")
    private Integer page;

    


    @Schema(description = "姣忛〉澶у皬", example = "20")
    private Integer size;

    


    @Schema(description = "鎬婚〉鏁?, example = "50")
    private Integer totalPages;

    


    @Schema(description = "鏄惁鏈変笅涓€椤?, example = "true")
    private Boolean hasNext;

    


    @Schema(description = "鏄惁鏈変笂涓€椤?, example = "false")
    private Boolean hasPrevious;

    


    @Schema(description = "鎼滅储鑰楁椂锛堟绉掞級", example = "50")
    private Long took;

    


    @Schema(description = "鑱氬悎缁撴灉")
    private Map<String, Object> aggregations;

    


    @Schema(description = "楂樹寒缁撴灉")
    private Map<String, List<String>> highlights;

    


    public static <T> SearchResult<T> of(List<T> list, Long total, Integer page, Integer size, Long took) {
        int totalPages = (int) Math.ceil((double) total / size);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;

        return SearchResult.<T>builder()
                .list(list)
                .total(total)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .took(took)
                .build();
    }

    


    public static <T> SearchResult<T> of(List<T> list, Long total, Integer page, Integer size, Long took,
                                         Map<String, Object> aggregations) {
        SearchResult<T> result = of(list, total, page, size, took);
        result.setAggregations(aggregations);
        return result;
    }

    


    public static <T> SearchResult<T> of(List<T> list, Long total, Integer page, Integer size, Long took,
                                         Map<String, Object> aggregations, Map<String, List<String>> highlights) {
        SearchResult<T> result = of(list, total, page, size, took, aggregations);
        result.setHighlights(highlights);
        return result;
    }
}
