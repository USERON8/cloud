package com.cloud.log.service;

import com.cloud.common.result.PageResult;

import java.util.Map;

/**
 * 日志服务接口
 * 提供统一的日志查询和管理功能
 *
 * @author what's up
 */
public interface LogService {

    /**
     * 获取日志列表
     *
     * @param page      页码
     * @param size      每页数量
     * @param level     日志级别
     * @param service   服务名称
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param keyword   关键词
     * @return 分页结果
     */
    PageResult<Object> getLogs(Integer page, Integer size, String level, String service, 
                              String startTime, String endTime, String keyword);

    /**
     * 获取应用日志
     *
     * @param page      页码
     * @param size      每页数量
     * @param appName   应用名称
     * @param level     日志级别
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 分页结果
     */
    PageResult<Object> getApplicationLogs(Integer page, Integer size, String appName, 
                                         String level, String startTime, String endTime);

    /**
     * 获取操作日志
     *
     * @param page          页码
     * @param size          每页数量
     * @param userId        用户ID
     * @param operationType 操作类型
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @return 分页结果
     */
    PageResult<Object> getOperationLogs(Integer page, Integer size, Long userId, 
                                       String operationType, String startTime, String endTime);

    /**
     * 获取错误日志
     *
     * @param page      页码
     * @param size      每页数量
     * @param service   服务名称
     * @param errorType 错误类型
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 分页结果
     */
    PageResult<Object> getErrorLogs(Integer page, Integer size, String service, 
                                   String errorType, String startTime, String endTime);

    /**
     * 获取访问日志
     *
     * @param page       页码
     * @param size       每页数量
     * @param userId     用户ID
     * @param path       请求路径
     * @param method     HTTP方法
     * @param statusCode 状态码
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 分页结果
     */
    PageResult<Object> getAccessLogs(Integer page, Integer size, Long userId, String path, 
                                    String method, Integer statusCode, String startTime, String endTime);

    /**
     * 获取审计日志
     *
     * @param page         页码
     * @param size         每页数量
     * @param userId       用户ID
     * @param resourceType 资源类型
     * @param action       操作类型
     * @param startTime    开始时间
     * @param endTime      结束时间
     * @return 分页结果
     */
    PageResult<Object> getAuditLogs(Integer page, Integer size, Long userId, String resourceType, 
                                   String action, String startTime, String endTime);

    /**
     * 获取日志统计信息
     *
     * @param dimension 统计维度
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计结果
     */
    Map<String, Object> getLogStatistics(String dimension, String startTime, String endTime);

    /**
     * 导出日志
     *
     * @param exportRequest 导出条件
     * @return 导出任务ID
     */
    String exportLogs(Map<String, Object> exportRequest);

    /**
     * 清理日志
     *
     * @param retentionDays 保留天数
     * @param logType       日志类型
     * @return 清理结果
     */
    boolean cleanupLogs(Integer retentionDays, String logType);

    /**
     * 根据ID获取日志详情
     *
     * @param id 日志ID
     * @return 日志详情
     */
    Object getLogById(String id);

    /**
     * 搜索日志
     *
     * @param keyword   搜索关键词
     * @param page      页码
     * @param size      每页数量
     * @param logType   日志类型
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 搜索结果
     */
    PageResult<Object> searchLogs(String keyword, Integer page, Integer size, String logType, 
                                 String startTime, String endTime);
}
