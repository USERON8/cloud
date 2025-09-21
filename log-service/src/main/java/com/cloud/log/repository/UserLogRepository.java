package com.cloud.log.repository;

import com.cloud.log.document.UserLogDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户日志ES仓储接口
 *
 * @author cloud
 * @date 2025/1/15
 */
@Repository
public interface UserLogRepository extends ElasticsearchRepository<UserLogDocument, String> {

    /**
     * 根据用户ID查询日志
     */
    Page<UserLogDocument> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据用户名查询日志
     */
    Page<UserLogDocument> findByUsernameContaining(String username, Pageable pageable);

    /**
     * 根据操作人查询日志
     */
    Page<UserLogDocument> findByOperator(String operator, Pageable pageable);

    /**
     * 根据变更类型查询日志
     */
    Page<UserLogDocument> findByChangeType(Integer changeType, Pageable pageable);

    /**
     * 根据时间范围查询日志
     */
    Page<UserLogDocument> findByOperateTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据追踪ID查询日志（用于幂等性检查）
     */
    List<UserLogDocument> findByTraceId(String traceId);
}
