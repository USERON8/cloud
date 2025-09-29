package com.cloud.log.controller;

import com.cloud.common.messaging.AsyncLogProducer;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.common.utils.UserContextUtils;
import com.cloud.log.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 日志RESTful API控制器
 * 提供日志资源的查询和管理接口
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@Validated
@Tag(name = "日志服务", description = "日志资源的RESTful API接口")
public class LogRestController {

    private final LogService logService;
    private final AsyncLogProducer asyncLogProducer;

    /**
     * 获取日志列表（支持分页和查询参数）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_log:read')")
    @Cacheable(cacheNames = "logQueryCache",
            key = "'logs:' + #page + ':' + #size + ':' + (#level != null ? #level : 'null') + ':' + (#service != null ? #service : 'null')")
    @Operation(summary = "获取日志列表", description = "获取日志列表，支持分页和查询参数")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PageResult.class)))
    public Result<PageResult<Object>> getLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1")
            @Min(1) Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) Integer size,
            @Parameter(description = "日志级别") @RequestParam(required = false) String level,
            @Parameter(description = "服务名称") @RequestParam(required = false) String service,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {

        try {
            log.debug("获取日志列表 - 页码: {}, 大小: {}, 级别: {}", page, size, level);
            PageResult<Object> result = logService.getLogs(page, size, level, service, startTime, endTime, keyword);

            // 记录日志查询操作
            recordLogOperation("GET_LOGS", result.getTotal());

            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取日志列表失败", e);
            return Result.error("获取日志列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取应用日志
     */
    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_log:read')")
    @Operation(summary = "获取应用日志", description = "获取应用程序日志")
    public Result<PageResult<Object>> getApplicationLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "应用名称") @RequestParam(required = false) String appName,
            @Parameter(description = "日志级别") @RequestParam(required = false) String level,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {

        try {
            log.debug("获取应用日志 - 应用: {}, 级别: {}", appName, level);
            PageResult<Object> result = logService.getApplicationLogs(page, size, appName, level, startTime, endTime);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取应用日志失败", e);
            return Result.error("获取应用日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取操作日志
     */
    @GetMapping("/operations")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_log:read')")
    @Operation(summary = "获取操作日志", description = "获取用户操作日志")
    public Result<PageResult<Object>> getOperationLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {

        try {
            log.debug("获取操作日志 - 用户ID: {}, 操作类型: {}", userId, operationType);
            PageResult<Object> result = logService.getOperationLogs(page, size, userId, operationType, startTime, endTime);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取操作日志失败", e);
            return Result.error("获取操作日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取错误日志
     */
    @GetMapping("/errors")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_log:read')")
    @Operation(summary = "获取错误日志", description = "获取系统错误日志")
    public Result<PageResult<Object>> getErrorLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "服务名称") @RequestParam(required = false) String service,
            @Parameter(description = "错误类型") @RequestParam(required = false) String errorType,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {

        try {
            log.debug("获取错误日志 - 服务: {}, 错误类型: {}", service, errorType);
            PageResult<Object> result = logService.getErrorLogs(page, size, service, errorType, startTime, endTime);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取错误日志失败", e);
            return Result.error("获取错误日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取访问日志
     */
    @GetMapping("/access")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_log:read')")
    @Operation(summary = "获取访问日志", description = "获取API访问日志")
    public Result<PageResult<Object>> getAccessLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "请求路径") @RequestParam(required = false) String path,
            @Parameter(description = "HTTP方法") @RequestParam(required = false) String method,
            @Parameter(description = "状态码") @RequestParam(required = false) Integer statusCode,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {

        try {
            log.debug("获取访问日志 - 用户ID: {}, 路径: {}", userId, path);
            PageResult<Object> result = logService.getAccessLogs(page, size, userId, path, method, statusCode, startTime, endTime);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取访问日志失败", e);
            return Result.error("获取访问日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取审计日志
     */
    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_log:audit')")
    @Operation(summary = "获取审计日志", description = "获取系统审计日志")
    public Result<PageResult<Object>> getAuditLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "资源类型") @RequestParam(required = false) String resourceType,
            @Parameter(description = "操作类型") @RequestParam(required = false) String action,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {

        try {
            log.debug("获取审计日志 - 用户ID: {}, 资源类型: {}, 操作: {}", userId, resourceType, action);
            PageResult<Object> result = logService.getAuditLogs(page, size, userId, resourceType, action, startTime, endTime);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取审计日志失败", e);
            return Result.error("获取审计日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取日志统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_log:read')")
    @Operation(summary = "获取日志统计", description = "获取日志统计信息")
    public Result<Map<String, Object>> getLogStatistics(
            @Parameter(description = "统计维度") @RequestParam(defaultValue = "level") String dimension,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {

        try {
            log.debug("获取日志统计 - 维度: {}", dimension);
            Map<String, Object> statistics = logService.getLogStatistics(dimension, startTime, endTime);
            return Result.success("查询成功", statistics);
        } catch (Exception e) {
            log.error("获取日志统计失败", e);
            return Result.error("获取日志统计失败: " + e.getMessage());
        }
    }

    /**
     * 导出日志
     */
    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_log:export')")
    @Operation(summary = "导出日志", description = "导出日志数据")
    public Result<String> exportLogs(
            @Parameter(description = "导出条件") @RequestBody Map<String, Object> exportRequest) {

        try {
            log.info("导出日志 - 条件: {}", exportRequest);
            String exportId = logService.exportLogs(exportRequest);
            return Result.success("导出任务已创建", exportId);
        } catch (Exception e) {
            log.error("导出日志失败", e);
            return Result.error("导出日志失败: " + e.getMessage());
        }
    }

    /**
     * 清理日志
     */
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_log:delete')")
    @Operation(summary = "清理日志", description = "清理过期日志数据")
    public Result<Boolean> cleanupLogs(
            @Parameter(description = "保留天数") @RequestParam(defaultValue = "30") Integer retentionDays,
            @Parameter(description = "日志类型") @RequestParam(required = false) String logType) {

        try {
            log.info("清理日志 - 保留天数: {}, 类型: {}", retentionDays, logType);
            boolean result = logService.cleanupLogs(retentionDays, logType);

            // 记录清理操作
            recordLogOperation("CLEANUP_LOGS", result ? 1L : 0L);

            return result ? Result.success("清理成功", true) : Result.error("清理失败");
        } catch (Exception e) {
            log.error("清理日志失败", e);
            return Result.error("清理日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取日志详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_log:read')")
    @Operation(summary = "获取日志详情", description = "根据ID获取日志详细信息")
    public Result<Object> getLogById(
            @Parameter(description = "日志ID") @PathVariable String id) {

        try {
            Object logDetail = logService.getLogById(id);
            if (logDetail == null) {
                return Result.error("日志不存在");
            }
            return Result.success("查询成功", logDetail);
        } catch (Exception e) {
            log.error("获取日志详情失败，日志ID: {}", id, e);
            return Result.error("获取日志详情失败: " + e.getMessage());
        }
    }

    /**
     * 搜索日志
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_log:read')")
    @Operation(summary = "搜索日志", description = "根据关键词搜索日志")
    public Result<PageResult<Object>> searchLogs(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "日志类型") @RequestParam(required = false) String logType,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {

        try {
            log.debug("搜索日志 - 关键词: {}", keyword);
            PageResult<Object> result = logService.searchLogs(keyword, page, size, logType, startTime, endTime);
            return Result.success("搜索成功", result);
        } catch (Exception e) {
            log.error("搜索日志失败 - 关键词: {}", keyword, e);
            return Result.error("搜索日志失败: " + e.getMessage());
        }
    }

    /**
     * 记录日志操作
     */
    private void recordLogOperation(String operation, Long resultCount) {
        try {
            asyncLogProducer.sendBusinessLogAsync(
                    "log-service",
                    "LOG_MANAGEMENT",
                    operation,
                    "日志管理操作",
                    operation,
                    "LOG",
                    null,
                    String.format("{\"operation\":\"%s\",\"resultCount\":%d}", operation, resultCount),
                    UserContextUtils.getCurrentUsername() != null ? UserContextUtils.getCurrentUsername() : "SYSTEM",
                    "日志操作: " + operation + ", 结果数: " + resultCount
            );
        } catch (Exception e) {
            log.warn("记录日志操作失败", e);
        }
    }

}
