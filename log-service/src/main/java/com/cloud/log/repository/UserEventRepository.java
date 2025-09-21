package com.cloud.log.repository;

import com.cloud.log.domain.document.UserEventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户事件Repository接口
 * 基于Spring Data Elasticsearch标准实现
 * 按照阿里巴巴官方示例标准设计
 *
 * @author cloud
 * @date 2025/1/15
 */
@Repository
public interface UserEventRepository extends ElasticsearchRepository<UserEventDocument, String> {

    /**
     * 根据用户ID查询所有事件（按事件时间倒序）
     */
    List<UserEventDocument> findByUserIdOrderByEventTimeDesc(Long userId);

    /**
     * 根据用户名查询事件（分页，按事件时间倒序）
     */
    Page<UserEventDocument> findByUsernameOrderByEventTimeDesc(String username, Pageable pageable);

    /**
     * 根据事件类型查询（分页，按事件时间倒序）
     */
    Page<UserEventDocument> findByEventTypeOrderByEventTimeDesc(String eventType, Pageable pageable);

    /**
     * 根据时间范围查询事件（分页，按事件时间倒序）
     */
    Page<UserEventDocument> findByEventTimeBetweenOrderByEventTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据用户状态查询事件（分页，按事件时间倒序）
     */
    Page<UserEventDocument> findByUserStatusOrderByEventTimeDesc(Integer userStatus, Pageable pageable);

    /**
     * 根据注册来源查询事件（分页，按事件时间倒序）
     */
    Page<UserEventDocument> findByRegisterSourceOrderByEventTimeDesc(Integer registerSource, Pageable pageable);

    /**
     * 根据VIP等级查询事件（分页，按事件时间倒序）
     */
    Page<UserEventDocument> findByVipLevelOrderByEventTimeDesc(Integer vipLevel, Pageable pageable);

    /**
     * 根据实名认证状态查询事件（分页，按事件时间倒序）
     */
    Page<UserEventDocument> findByVerificationStatusOrderByEventTimeDesc(Integer verificationStatus, Pageable pageable);

    /**
     * 根据操作类型查询事件（分页，按事件时间倒序）
     */
    Page<UserEventDocument> findByOperationTypeOrderByEventTimeDesc(Integer operationType, Pageable pageable);

    /**
     * 根据用户ID和事件类型查询（按事件时间倒序）
     */
    List<UserEventDocument> findByUserIdAndEventTypeOrderByEventTimeDesc(Long userId, String eventType);

    /**
     * 根据TraceId查询事件
     */
    Optional<UserEventDocument> findByTraceId(String traceId);

    /**
     * 检查TraceId是否存在
     */
    boolean existsByTraceId(String traceId);

    /**
     * 检查用户ID和事件类型的组合是否存在
     */
    boolean existsByUserIdAndEventType(Long userId, String eventType);

    /**
     * 统计时间范围内的事件数量
     */
    long countByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定用户的事件数量
     */
    long countByUserId(Long userId);

    /**
     * 统计指定事件类型的数量
     */
    long countByEventType(String eventType);

    /**
     * 统计指定注册来源的用户数量
     */
    long countByRegisterSource(Integer registerSource);

    /**
     * 统计指定VIP等级的用户数量
     */
    long countByVipLevel(Integer vipLevel);

    /**
     * 统计指定用户状态的用户数量
     */
    long countByUserStatus(Integer userStatus);

    /**
     * 统计指定实名认证状态的用户数量
     */
    long countByVerificationStatus(Integer verificationStatus);

    /**
     * 统计指定操作类型的事件数量
     */
    long countByOperationType(Integer operationType);

    /**
     * 删除指定时间之前的事件
     */
    long deleteByEventTimeBefore(LocalDateTime expiredTime);

    /**
     * 根据性别查询事件
     */
    Page<UserEventDocument> findByGenderOrderByEventTimeDesc(Integer gender, Pageable pageable);

    /**
     * 根据操作人ID查询事件
     */
    Page<UserEventDocument> findByOperatorIdOrderByEventTimeDesc(Long operatorId, Pageable pageable);

    /**
     * 根据地区编码查询事件
     */
    Page<UserEventDocument> findByRegionCodeOrderByEventTimeDesc(String regionCode, Pageable pageable);
}
