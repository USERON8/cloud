package com.cloud.log.service;

import com.cloud.log.domain.document.UserEventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户事件服务接口
 * 负责用户事件的存储和查询
 * 基于阿里巴巴官方示例标准设计
 *
 * @author cloud
 * @date 2025/1/15
 */
public interface UserEventService {

    /**
     * 保存用户事件
     */
    void saveUserEvent(UserEventDocument document);

    /**
     * 检查用户事件是否已存在（幂等性检查）
     */
    boolean existsByUserIdAndEventType(Long userId, String eventType, String traceId);

    /**
     * 根据ID查询用户事件
     */
    Optional<UserEventDocument> findById(String id);

    /**
     * 根据用户ID查询所有事件
     */
    List<UserEventDocument> findByUserId(Long userId);

    /**
     * 根据用户名查询事件
     */
    Page<UserEventDocument> findByUsername(String username, Pageable pageable);

    /**
     * 根据事件类型查询
     */
    Page<UserEventDocument> findByEventType(String eventType, Pageable pageable);

    /**
     * 根据时间范围查询
     */
    Page<UserEventDocument> findByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据用户状态查询
     */
    Page<UserEventDocument> findByUserStatus(Integer userStatus, Pageable pageable);

    /**
     * 根据注册来源查询
     */
    Page<UserEventDocument> findByRegisterSource(Integer registerSource, Pageable pageable);

    /**
     * 根据VIP等级查询
     */
    Page<UserEventDocument> findByVipLevel(Integer vipLevel, Pageable pageable);

    /**
     * 根据实名认证状态查询
     */
    Page<UserEventDocument> findByVerificationStatus(Integer verificationStatus, Pageable pageable);

    /**
     * 根据操作类型查询
     */
    Page<UserEventDocument> findByOperationType(Integer operationType, Pageable pageable);

    /**
     * 根据TraceId查询
     */
    Optional<UserEventDocument> findByTraceId(String traceId);

    /**
     * 统计指定时间范围内的事件数量
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
     * 删除过期的事件记录
     */
    void deleteExpiredEvents(LocalDateTime expiredTime);
}
