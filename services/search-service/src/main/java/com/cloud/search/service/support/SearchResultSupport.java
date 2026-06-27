package com.cloud.search.service.support;

import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResultDTO;
import com.cloud.search.service.ElasticsearchOptimizedService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchResultSupport {

  private static final List<DateTimeFormatter> SUPPORTED_DATE_TIME_FORMATTERS =
      List.of(
          DateTimeFormatter.ISO_LOCAL_DATE_TIME,
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

  private final ObjectMapper objectMapper;

  public ProductSearchRequest copyRequest(ProductSearchRequest request) {
    if (request == null) {
      return new ProductSearchRequest();
    }
    return objectMapper.convertValue(request, ProductSearchRequest.class);
  }

  public SearchResultDTO<ProductDocument> toSearchResultDTO(
      ElasticsearchOptimizedService.SearchResultDTO esResult,
      int page,
      int size,
      long took,
      boolean usedSearchAfter) {
    List<ProductDocument> list =
        esResult == null || esResult.getDocuments() == null
            ? Collections.emptyList()
            : esResult.getDocuments().stream().map(this::toProductDocument).toList();
    long total = esResult == null ? list.size() : esResult.getTotal();
    int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
    boolean hasPrevious = usedSearchAfter || page > 0;
    boolean hasNext =
        usedSearchAfter
            ? esResult != null
                && esResult.getSearchAfter() != null
                && !esResult.getSearchAfter().isEmpty()
            : page < totalPages - 1;
    return SearchResultDTO.<ProductDocument>builder()
        .list(list)
        .total(total)
        .page(page)
        .size(size)
        .totalPages(totalPages)
        .hasNext(hasNext)
        .hasPrevious(hasPrevious)
        .took(took)
        .aggregations(esResult == null ? Map.of() : esResult.getAggregations())
        .highlights(esResult == null ? Map.of() : esResult.getHighlights())
        .searchAfter(esResult == null ? List.of() : esResult.getSearchAfter())
        .build();
  }

  public Map<String, Object> normalizeProductAggregations(Map<String, Object> rawAggregations) {
    if (rawAggregations == null || rawAggregations.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> normalized = new LinkedHashMap<>();
    normalized.put("categories", normalizeBucketCounts(rawAggregations.get("categories")));
    normalized.put("brands", normalizeBucketCounts(rawAggregations.get("brands")));
    if (rawAggregations.containsKey("priceRanges")) {
      normalized.put("priceRanges", rawAggregations.get("priceRanges"));
    }
    return normalized;
  }

  public ProductDocument toProductDocument(Object document) {
    if (document instanceof ProductDocument productDocument) {
      return productDocument;
    }
    if (document instanceof Map<?, ?> source) {
      Map<String, Object> normalized = new LinkedHashMap<>();
      LocalDateTime createdAt = null;
      LocalDateTime updatedAt = null;
      for (Map.Entry<?, ?> entry : source.entrySet()) {
        if (entry.getKey() == null) {
          continue;
        }
        String key = String.valueOf(entry.getKey());
        Object value = entry.getValue();
        if ("createdAt".equals(key)) {
          createdAt = parseDateTimeValue(value);
          normalized.put(key, createdAt);
          continue;
        }
        if ("updatedAt".equals(key)) {
          updatedAt = parseDateTimeValue(value);
          normalized.put(key, updatedAt);
          continue;
        }
        normalized.put(key, value);
      }
      ProductDocument mapped = objectMapper.convertValue(normalized, ProductDocument.class);
      if (createdAt != null) {
        mapped.setCreatedAt(createdAt);
      }
      if (updatedAt != null) {
        mapped.setUpdatedAt(updatedAt);
      }
      return mapped;
    }
    return objectMapper.convertValue(document, ProductDocument.class);
  }

  private Map<String, Long> normalizeBucketCounts(Object rawBuckets) {
    if (rawBuckets instanceof Map<?, ?> bucketMap) {
      Map<String, Long> normalized = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : bucketMap.entrySet()) {
        String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey()).trim();
        if (key.isEmpty() || entry.getValue() == null) {
          continue;
        }
        if (entry.getValue() instanceof Number number) {
          normalized.put(key, number.longValue());
        }
      }
      return normalized;
    }
    if (!(rawBuckets instanceof List<?> bucketList)) {
      return Map.of();
    }
    Map<String, Long> normalized = new LinkedHashMap<>();
    for (Object bucket : bucketList) {
      if (!(bucket instanceof Map<?, ?> bucketMap)) {
        continue;
      }
      Object keyValue = bucketMap.get("key");
      Object countValue = bucketMap.get("count");
      if (keyValue == null || countValue == null) {
        continue;
      }
      String key = String.valueOf(keyValue).trim();
      if (key.isEmpty()) {
        continue;
      }
      long count;
      if (countValue instanceof Number number) {
        count = number.longValue();
      } else {
        try {
          count = Long.parseLong(String.valueOf(countValue));
        } catch (NumberFormatException ex) {
          continue;
        }
      }
      normalized.put(key, count);
    }
    return normalized;
  }

  private LocalDateTime parseDateTimeValue(Object value) {
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime;
    }
    if (!(value instanceof String text)) {
      return null;
    }
    String normalized = text.trim();
    if (normalized.isEmpty()) {
      return null;
    }
    for (DateTimeFormatter formatter : SUPPORTED_DATE_TIME_FORMATTERS) {
      try {
        return LocalDateTime.parse(normalized, formatter);
      } catch (DateTimeParseException ignored) {
      }
    }
    return null;
  }
}
