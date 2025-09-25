package com.cloud.log.service.impl;

import com.cloud.log.domain.document.UserEventDocument;
import com.cloud.log.repository.UserEventRepository;
import com.cloud.log.service.ElasticsearchOptimizedService;
import com.cloud.log.service.UserEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户事件服务实现类
 * 负责用户事件的存储和查询
 * 基于阿里巴巴官方示例标准实现
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventServiceImpl implements UserEventService {

    private final UserEventRepository userEventRepository;
    private final ElasticsearchOptimizedService elasticsearchOptimizedService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"userEventCache", "userEventListCache"}, allEntries = true)
    public void saveUserEvent(UserEventDocument document) {
        try {
            // 使用优化的ES服务进行高性能写入
            boolean success = elasticsearchOptimizedService.indexDocument(
                    "user_event_index",
                    document.getId(),
                    document
            );

            if (success) {
                log.debug("用户事件保存成功 - 用户ID: {}, 事件类型: {}, 文档ID: {}",
                        document.getUserId(), document.getEventType(), document.getId());
            } else {
                log.error("用户事件保存失败 - 用户ID: {}, 事件类型: {}, 文档ID: {}",
                        document.getUserId(), document.getEventType(), document.getId());
                throw new RuntimeException("用户事件保存失败");
            }
        } catch (Exception e) {
            log.error("保存用户事件失败 - 用户ID: {}, 事件类型: {}, 错误: {}",
                    document.getUserId(), document.getEventType(), e.getMessage(), e);
            throw new RuntimeException("保存用户事件失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userEventExistsCache",
               key = "#traceId != null ? 'trace:' + #traceId : 'user:' + #userId + ':' + #eventType",
               condition = "#userId != null || #traceId != null")
    public boolean existsByUserIdAndEventType(Long userId, String eventType, String traceId) {
        try {
            // 优先使用traceId检查，因为它更唯一
            if (traceId != null) {
                return userEventRepository.existsByTraceId(traceId);
            }
            // 备用检查方案
            return userEventRepository.existsByUserIdAndEventType(userId, eventType);
        } catch (Exception e) {
            log.error("检查用户事件是否存在时发生异常 - 用户ID: {}, 事件类型: {}, TraceId: {}, 错误: {}",
                    userId, eventType, traceId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userEventCache", key = "#id", condition = "#id != null")
    public Optional<UserEventDocument> findById(String id) {
        try {
            return userEventRepository.findById(id);
        } catch (Exception e) {
            log.error("根据ID查询用户事件失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userEventListCache", key = "#userId", condition = "#userId != null")
    public List<UserEventDocument> findByUserId(Long userId) {
        try {
            return userEventRepository.findByUserIdOrderByEventTimeDesc(userId);
        } catch (Exception e) {
            log.error("根据用户ID查询事件失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public Page<UserEventDocument> findByUsername(String username, Pageable pageable) {
        try {
            return userEventRepository.findByUsernameOrderByEventTimeDesc(username, pageable);
        } catch (Exception e) {
            log.error("根据用户名查询事件失败 - 用户名: {}, 错误: {}", username, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<UserEventDocument> findByEventType(String eventType, Pageable pageable) {
        try {
            return userEventRepository.findByEventTypeOrderByEventTimeDesc(eventType, pageable);
        } catch (Exception e) {
            log.error("根据事件类型查询失败 - 事件类型: {}, 错误: {}", eventType, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<UserEventDocument> findByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        try {
            return userEventRepository.findByEventTimeBetweenOrderByEventTimeDesc(startTime, endTime, pageable);
        } catch (Exception e) {
            log.error("根据时间范围查询用户事件失败 - 开始时间: {}, 结束时间: {}, 错误: {}",
                    startTime, endTime, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<UserEventDocument> findByUserStatus(Integer userStatus, Pageable pageable) {
        try {
            return userEventRepository.findByUserStatusOrderByEventTimeDesc(userStatus, pageable);
        } catch (Exception e) {
            log.error("根据用户状态查询事件失败 - 用户状态: {}, 错误: {}", userStatus, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<UserEventDocument> findByRegisterSource(Integer registerSource, Pageable pageable) {
        try {
            return userEventRepository.findByRegisterSourceOrderByEventTimeDesc(registerSource, pageable);
        } catch (Exception e) {
            log.error("根据注册来源查询事件失败 - 注册来源: {}, 错误: {}", registerSource, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<UserEventDocument> findByVipLevel(Integer vipLevel, Pageable pageable) {
        try {
            return userEventRepository.findByVipLevelOrderByEventTimeDesc(vipLevel, pageable);
        } catch (Exception e) {
            log.error("根据VIP等级查询事件失败 - VIP等级: {}, 错误: {}", vipLevel, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<UserEventDocument> findByVerificationStatus(Integer verificationStatus, Pageable pageable) {
        try {
            return userEventRepository.findByVerificationStatusOrderByEventTimeDesc(verificationStatus, pageable);
        } catch (Exception e) {
            log.error("根据实名认证状态查询事件失败 - 认证状态: {}, 错误: {}", verificationStatus, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Page<UserEventDocument> findByOperationType(Integer operationType, Pageable pageable) {
        try {
            return userEventRepository.findByOperationTypeOrderByEventTimeDesc(operationType, pageable);
        } catch (Exception e) {
            log.error("根据操作类型查询事件失败 - 操作类型: {}, 错误: {}", operationType, e.getMessage(), e);
            return Page.empty();
        }
    }

    @Override
    public Optional<UserEventDocument> findByTraceId(String traceId) {
        try {
            return userEventRepository.findByTraceId(traceId);
        } catch (Exception e) {
            log.error("根据TraceId查询用户事件失败 - TraceId: {}, 错误: {}", traceId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public long countByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return userEventRepository.countByEventTimeBetween(startTime, endTime);
        } catch (Exception e) {
            log.error("统计时间范围内用户事件数量失败 - 开始时间: {}, 结束时间: {}, 错误: {}",
                    startTime, endTime, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByUserId(Long userId) {
        try {
            return userEventRepository.countByUserId(userId);
        } catch (Exception e) {
            log.error("统计用户事件数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByEventType(String eventType) {
        try {
            return userEventRepository.countByEventType(eventType);
        } catch (Exception e) {
            log.error("统计事件类型数量失败 - 事件类型: {}, 错误: {}", eventType, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByRegisterSource(Integer registerSource) {
        try {
            return userEventRepository.countByRegisterSource(registerSource);
        } catch (Exception e) {
            log.error("统计注册来源用户数量失败 - 注册来源: {}, 错误: {}", registerSource, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByVipLevel(Integer vipLevel) {
        try {
            return userEventRepository.countByVipLevel(vipLevel);
        } catch (Exception e) {
            log.error("统计VIP等级用户数量失败 - VIP等级: {}, 错误: {}", vipLevel, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public void deleteExpiredEvents(LocalDateTime expiredTime) {
        try {
            long count = userEventRepository.deleteByEventTimeBefore(expiredTime);
            log.info("删除过期用户事件完成 - 删除数量: {}, 过期时间: {}", count, expiredTime);
        } catch (Exception e) {
            log.error("删除过期用户事件失败 - 过期时间: {}, 错误: {}", expiredTime, e.getMessage(), e);
            throw new RuntimeException("删除过期用户事件失败", e);
        }
    }
}
