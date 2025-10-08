package com.cloud.log.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 日志服务ES操作优化服务
 * 提供高性能的ES读写操作，针对日志场景进行优化
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchOptimizedService {

    private static final int BULK_SIZE = 1000; // 批量操作大小
    private static final int BULK_TIMEOUT_SECONDS = 30; // 批量操作超时时间
    private final ElasticsearchClient elasticsearchClient;
    // log-service不使用Redis，使用内存缓存替代
    private final java.util.concurrent.ConcurrentHashMap<String, Long> indexExistsCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 批量写入文档 - 高性能优化版本
     * 使用bulk API进行批量写入，提高写入性能
     *
     * @param indexName 索引名称
     * @param documents 文档列表
     * @param <T>       文档类型
     * @return 成功写入的文档数量
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> int bulkIndex(String indexName, List<T> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("批量写入文档列表为空，跳过操作");
            return 0;
        }

        try {
            log.info("开始批量写入文档到索引: {}, 文档数量: {}", indexName, documents.size());

            // 确保索引存在
            ensureIndexExists(indexName);

            // 分批处理大量文档
            int totalSuccess = 0;
            int batchCount = (documents.size() + BULK_SIZE - 1) / BULK_SIZE;

            for (int i = 0; i < batchCount; i++) {
                int start = i * BULK_SIZE;
                int end = Math.min(start + BULK_SIZE, documents.size());
                List<T> batch = documents.subList(start, end);

                int batchSuccess = processBulkBatch(indexName, batch, i + 1, batchCount);
                totalSuccess += batchSuccess;
            }

            log.info("批量写入完成 - 索引: {}, 总文档数: {}, 成功写入: {}",
                    indexName, documents.size(), totalSuccess);
            return totalSuccess;

        } catch (Exception e) {
            log.error("批量写入文档失败 - 索引: {}, 错误: {}", indexName, e.getMessage(), e);
            throw new RuntimeException("批量写入文档失败", e);
        }
    }

    /**
     * 处理单个批次的批量操作
     */
    private <T> int processBulkBatch(String indexName, List<T> batch, int batchNum, int totalBatches) {
        try {
            log.debug("处理批次 {}/{} - 文档数量: {}", batchNum, totalBatches, batch.size());

            // 构建批量操作
            List<BulkOperation> operations = new ArrayList<>();
            for (T document : batch) {
                BulkOperation operation = BulkOperation.of(b -> b
                        .index(idx -> idx
                                .index(indexName)
                                .document(document)
                        )
                );
                operations.add(operation);
            }

            // 执行批量操作
            BulkRequest bulkRequest = BulkRequest.of(b -> b
                    .operations(operations)
                    .refresh(Refresh.False) // 不立即刷新，提高性能
            );

            BulkResponse bulkResponse = elasticsearchClient.bulk(bulkRequest);

            // 处理响应
            int successCount = 0;
            int errorCount = 0;

            for (BulkResponseItem item : bulkResponse.items()) {
                if (item.error() != null) {
                    errorCount++;
                    log.warn("批量操作项失败 - 索引: {}, ID: {}, 错误: {}",
                            item.index(), item.id(), item.error().reason());
                } else {
                    successCount++;
                }
            }

            if (errorCount > 0) {
                log.warn("批次 {}/{} 部分失败 - 成功: {}, 失败: {}",
                        batchNum, totalBatches, successCount, errorCount);
            } else {
                log.debug("批次 {}/{} 全部成功 - 文档数量: {}",
                        batchNum, totalBatches, successCount);
            }

            return successCount;

        } catch (Exception e) {
            log.error("处理批次失败 - 批次: {}/{}, 错误: {}", batchNum, totalBatches, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 高性能单文档写入
     * 使用异步刷新策略提高性能
     *
     * @param indexName  索引名称
     * @param documentId 文档ID
     * @param document   文档内容
     * @param <T>        文档类型
     * @return 是否写入成功
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> boolean indexDocument(String indexName, String documentId, T document) {
        try {
            log.debug("写入单个文档 - 索引: {}, ID: {}", indexName, documentId);

            // 确保索引存在
            ensureIndexExists(indexName);

            IndexRequest<T> request = IndexRequest.of(i -> i
                    .index(indexName)
                    .id(documentId)
                    .document(document)
                    .refresh(Refresh.False) // 异步刷新，提高性能
            );

            IndexResponse response = elasticsearchClient.index(request);

            boolean success = response.result() == Result.Created || response.result() == Result.Updated;
            if (success) {
                log.debug("文档写入成功 - 索引: {}, ID: {}, 结果: {}",
                        indexName, documentId, response.result());
            } else {
                log.warn("文档写入异常 - 索引: {}, ID: {}, 结果: {}",
                        indexName, documentId, response.result());
            }

            return success;

        } catch (Exception e) {
            log.error("写入文档失败 - 索引: {}, ID: {}, 错误: {}",
                    indexName, documentId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 高性能搜索查询
     * 支持分页和排序
     *
     * @param indexName 索引名称
     * @param query     查询条件
     * @param from      起始位置
     * @param size      查询数量
     * @param clazz     文档类型
     * @param <T>       文档类型
     * @return 查询结果
     */
    @Transactional(readOnly = true)
    public <T> SearchResult<T> search(String indexName, Map<String, Object> query,
                                      int from, int size, Class<T> clazz) {
        try {
            log.debug("执行搜索查询 - 索引: {}, from: {}, size: {}", indexName, from, size);

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(indexName)
                    .from(from)
                    .size(size)
                    .source(src -> src.fetch(true))
            );

            SearchResponse<T> response = elasticsearchClient.search(searchRequest, clazz);

            List<T> documents = new ArrayList<>();
            for (Hit<T> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    documents.add(hit.source());
                }
            }

            long total = response.hits().total() != null ? response.hits().total().value() : 0;

            log.debug("搜索查询完成 - 索引: {}, 总数: {}, 返回: {}",
                    indexName, total, documents.size());

            return new SearchResult<>(documents, total, from, size);

        } catch (Exception e) {
            log.error("搜索查询失败 - 索引: {}, 错误: {}", indexName, e.getMessage(), e);
            return new SearchResult<>(List.of(), 0, from, size);
        }
    }

    /**
     * 确保索引存在
     */
    private void ensureIndexExists(String indexName) {
        try {
            // 检查内存缓存（简单的时间戳缓存，1小时有效）
            Long cachedTime = indexExistsCache.get(indexName);
            if (cachedTime != null && (System.currentTimeMillis() - cachedTime) < 3600000) { // 1小时
                return;
            }

            // 检查索引是否存在
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(indexName));
            boolean exists = elasticsearchClient.indices().exists(existsRequest).value();

            if (!exists) {
                log.info("索引不存在，创建索引: {}", indexName);
                createIndex(indexName);
            }

            // 缓存索引存在状态（使用当前时间戳）
            indexExistsCache.put(indexName, System.currentTimeMillis());

        } catch (Exception e) {
            log.error("检查/创建索引失败 - 索引: {}, 错误: {}", indexName, e.getMessage(), e);
        }
    }

    /**
     * 创建索引
     */
    private void createIndex(String indexName) throws IOException {
        CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c
                .index(indexName)
                .settings(s -> s
                        .numberOfShards("3")
                        .numberOfReplicas("1")
                        .refreshInterval(t -> t.time("30s")) // 日志场景：延长刷新间隔提高写入性能
                        .maxResultWindow(50000)
                )
        );

        elasticsearchClient.indices().create(createRequest);
        log.info("索引创建成功: {}", indexName);
    }

    /**
     * 强制刷新索引
     * 在需要立即查询刚写入的数据时使用
     */
    public void refreshIndex(String indexName) {
        try {
            elasticsearchClient.indices().refresh(r -> r.index(indexName));
            log.debug("索引刷新完成: {}", indexName);
        } catch (Exception e) {
            log.error("索引刷新失败 - 索引: {}, 错误: {}", indexName, e.getMessage(), e);
        }
    }

    /**
     * 搜索结果封装类
     */
    public static class SearchResult<T> {
        private final List<T> documents;
        private final long total;
        private final int from;
        private final int size;

        public SearchResult(List<T> documents, long total, int from, int size) {
            this.documents = documents;
            this.total = total;
            this.from = from;
            this.size = size;
        }

        public List<T> getDocuments() {
            return documents;
        }

        public long getTotal() {
            return total;
        }

        public int getFrom() {
            return from;
        }

        public int getSize() {
            return size;
        }

        public boolean hasMore() {
            return from + size < total;
        }
    }
}
