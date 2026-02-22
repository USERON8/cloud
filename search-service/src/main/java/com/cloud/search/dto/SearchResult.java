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
@Schema(description = "Search result")
public class SearchResult<T> {

    @Schema(description = "Current page records")
    private List<T> list;

    @Schema(description = "Total records")
    private Long total;

    @Schema(description = "Current page")
    private Integer page;

    @Schema(description = "Page size")
    private Integer size;

    @Schema(description = "Total pages")
    private Integer totalPages;

    @Schema(description = "Has next page")
    private Boolean hasNext;

    @Schema(description = "Has previous page")
    private Boolean hasPrevious;

    @Schema(description = "Query time in milliseconds")
    private Long took;

    @Schema(description = "Aggregation data")
    private Map<String, Object> aggregations;

    @Schema(description = "Highlight data")
    private Map<String, List<String>> highlights;

    public static <T> SearchResult<T> of(List<T> list, Long total, Integer page, Integer size, Long took) {
        int safeSize = size == null || size <= 0 ? 20 : size;
        int safePage = page == null || page < 0 ? 0 : page;
        long safeTotal = total == null ? 0L : total;
        int totalPages = (int) Math.ceil((double) safeTotal / safeSize);
        boolean hasNext = safePage < totalPages - 1;
        boolean hasPrevious = safePage > 0;

        return SearchResult.<T>builder()
                .list(list)
                .total(safeTotal)
                .page(safePage)
                .size(safeSize)
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
