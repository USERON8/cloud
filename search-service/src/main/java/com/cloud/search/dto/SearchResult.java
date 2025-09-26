package com.cloud.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 搜索结果响应
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索结果响应")
public class SearchResult<T> {

    /**
     * 搜索结果列表
     */
    @Schema(description = "搜索结果列表")
    private List<T> list;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数", example = "1000")
    private Long total;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码", example = "0")
    private Integer page;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "20")
    private Integer size;

    /**
     * 总页数
     */
    @Schema(description = "总页数", example = "50")
    private Integer totalPages;

    /**
     * 是否有下一页
     */
    @Schema(description = "是否有下一页", example = "true")
    private Boolean hasNext;

    /**
     * 是否有上一页
     */
    @Schema(description = "是否有上一页", example = "false")
    private Boolean hasPrevious;

    /**
     * 搜索耗时（毫秒）
     */
    @Schema(description = "搜索耗时（毫秒）", example = "50")
    private Long took;

    /**
     * 聚合结果
     */
    @Schema(description = "聚合结果")
    private Map<String, Object> aggregations;

    /**
     * 高亮结果
     */
    @Schema(description = "高亮结果")
    private Map<String, List<String>> highlights;

    /**
     * 构建分页信息
     */
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

    /**
     * 构建带聚合信息的分页结果
     */
    public static <T> SearchResult<T> of(List<T> list, Long total, Integer page, Integer size, Long took, 
                                        Map<String, Object> aggregations) {
        SearchResult<T> result = of(list, total, page, size, took);
        result.setAggregations(aggregations);
        return result;
    }

    /**
     * 构建带高亮的分页结果
     */
    public static <T> SearchResult<T> of(List<T> list, Long total, Integer page, Integer size, Long took,
                                        Map<String, Object> aggregations, Map<String, List<String>> highlights) {
        SearchResult<T> result = of(list, total, page, size, took, aggregations);
        result.setHighlights(highlights);
        return result;
    }
}
