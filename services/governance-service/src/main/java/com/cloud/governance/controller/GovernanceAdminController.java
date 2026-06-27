package com.cloud.governance.controller;

import com.cloud.api.user.UserGovernanceDubboApi;
import com.cloud.api.user.UserNotificationGovernanceDubboApi;
import com.cloud.common.domain.dto.governance.OutboxBatchRequeueRequestDTO;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.common.domain.dto.user.UserNotificationBatchRequestDTO;
import com.cloud.common.domain.dto.user.UserNotificationStatusChangeRequestDTO;
import com.cloud.common.domain.dto.user.UserSystemAnnouncementRequestDTO;
import com.cloud.common.domain.dto.user.UserUpsertRequestDTO;
import com.cloud.common.domain.vo.auth.TokenBlacklistStatsVO;
import com.cloud.common.domain.vo.governance.ThreadPoolMetricsVO;
import com.cloud.common.domain.vo.user.AdminPageVO;
import com.cloud.common.domain.vo.user.UserPageVO;
import com.cloud.common.domain.vo.user.UserStatisticsVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.remote.RemoteCallSupport;
import com.cloud.common.result.Result;
import com.cloud.common.threadpool.ThreadPoolResponseMapper;
import com.cloud.common.util.DateRangeValidator;
import com.cloud.governance.service.AuthGovernanceManagementService;
import com.cloud.governance.service.GovernanceUserManagementService;
import com.cloud.governance.service.MqGovernanceAggregationService;
import com.cloud.governance.service.ObservabilityEntryService;
import com.cloud.governance.service.OutboxGovernanceAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "治理管理员接口", description = "治理服务承载的管理员接口")
/**
 * 后台治理公开聚合入口。
 *
 * <p>这里承载后台页面直接调用的 /api/admin/** 路由。后续新增治理页面应优先放在这里，并通过 Dubbo 或受控内部客户端访问业务服务。
 */
public class GovernanceAdminController {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserGovernanceDubboApi userGovernanceDubboApi;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserNotificationGovernanceDubboApi userNotificationGovernanceDubboApi;

  private final RemoteCallSupport remoteCallSupport;
  private final GovernanceUserManagementService governanceUserManagementService;
  private final AuthGovernanceManagementService authGovernanceManagementService;
  private final MqGovernanceAggregationService mqGovernanceAggregationService;
  private final OutboxGovernanceAggregationService outboxGovernanceAggregationService;
  private final ObservabilityEntryService observabilityEntryService;

  @GetMapping("/api/admin/statistics/overview")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "通过治理服务获取统计总览")
  public Result<UserStatisticsVO> getStatisticsOverview() {
    return Result.success(
        "query successful",
        remoteCallSupport.query(
            "user-service.governance.getStatisticsOverview",
            userGovernanceDubboApi::getStatisticsOverview));
  }

  @GetMapping("/api/admin/statistics/overview/async")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "通过治理服务异步获取统计总览")
  public CompletableFuture<Result<UserStatisticsVO>> getStatisticsOverviewAsync() {
    return CompletableFuture.completedFuture(getStatisticsOverview());
  }

  @GetMapping("/api/admin/statistics/registration-trend")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "通过治理服务获取注册趋势")
  public Result<Map<LocalDate, Long>> getRegistrationTrend(
      @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate) {
    DateRangeValidator.validateInclusiveRange(startDate, endDate, 365);
    return Result.success(
        "query successful",
        remoteCallSupport.query(
            "user-service.governance.getRegistrationTrend",
            () -> userGovernanceDubboApi.getRegistrationTrend(startDate, endDate)));
  }

  @GetMapping("/api/admin/statistics/registration-trend/async")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "通过治理服务异步获取注册趋势")
  public CompletableFuture<Result<Map<LocalDate, Long>>> getRegistrationTrendAsync(
      @RequestParam(defaultValue = "30") @Min(1) @Max(365) Integer days) {
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusDays(days.longValue() - 1L);
    return CompletableFuture.completedFuture(getRegistrationTrend(startDate, endDate));
  }

  @GetMapping("/api/admin/statistics/role-distribution")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Long>> getRoleDistribution() {
    return Result.success(
        "query successful",
        remoteCallSupport.query(
            "user-service.governance.getRoleDistribution",
            userGovernanceDubboApi::getRoleDistribution));
  }

  @GetMapping("/api/admin/statistics/status-distribution")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Long>> getStatusDistribution() {
    return Result.success(
        "query successful",
        remoteCallSupport.query(
            "user-service.governance.getStatusDistribution",
            userGovernanceDubboApi::getStatusDistribution));
  }

  @GetMapping("/api/admin/statistics/active-users")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Long> countActiveUsers(
      @RequestParam(defaultValue = "7") @Min(1) @Max(365) Integer days) {
    return Result.success(
        "query successful",
        remoteCallSupport.query(
            "user-service.governance.countActiveUsers",
            () -> userGovernanceDubboApi.countActiveUsers(days)));
  }

  @GetMapping("/api/admin/statistics/growth-rate")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Double> calculateGrowthRate(
      @RequestParam(defaultValue = "7") @Min(1) @Max(365) Integer days) {
    return Result.success(
        "query successful",
        remoteCallSupport.query(
            "user-service.governance.calculateGrowthRate",
            () -> userGovernanceDubboApi.calculateGrowthRate(days)));
  }

  @GetMapping("/api/admin/statistics/activity-ranking")
  @PreAuthorize("hasAuthority('admin:all')")
  public CompletableFuture<Result<Map<Long, Long>>> getActivityRanking(
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer limit,
      @RequestParam(defaultValue = "30") @Min(1) @Max(365) Integer days) {
    Result<Map<Long, Long>> result =
        Result.success(
            "query successful",
            remoteCallSupport.query(
                "user-service.governance.getActivityRanking",
                () -> userGovernanceDubboApi.getActivityRanking(limit, days)));
    return CompletableFuture.completedFuture(result);
  }

  @PostMapping("/api/admin/statistics/cache-refreshes")
  @PreAuthorize("hasAuthority('admin:all')")
  public CompletableFuture<Result<Boolean>> refreshStatisticsCache() {
    Result<Boolean> result =
        Result.success(
            "cache refresh completed",
            remoteCallSupport.command(
                "user-service.governance.refreshStatisticsCache",
                userGovernanceDubboApi::refreshStatisticsCache));
    return CompletableFuture.completedFuture(result);
  }

  @GetMapping("/api/admin/thread-pools")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<List<Map<String, Object>>> getAllThreadPoolInfo() {
    List<Map<String, Object>> items =
        remoteCallSupport
            .query(
                "user-service.governance.getThreadPoolInfoList",
                userGovernanceDubboApi::getThreadPoolInfoList)
            .stream()
            .map(ThreadPoolResponseMapper::toResponse)
            .toList();
    return Result.success(items);
  }

  @GetMapping("/api/admin/thread-pools/{name}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getThreadPoolInfoByName(
      @PathVariable @Parameter(description = "线程池 Bean 名称") String name) {
    ThreadPoolMetricsVO metrics =
        remoteCallSupport.query(
            "user-service.governance.getThreadPoolInfo",
            () -> userGovernanceDubboApi.getThreadPoolInfo(name));
    if (metrics == null) {
      throw new BizException(ResultCode.NOT_FOUND, "Thread pool bean not found: " + name);
    }
    return Result.success(ThreadPoolResponseMapper.toResponse(metrics));
  }

  @GetMapping("/api/admins")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<AdminPageVO> getAdmins(
      @RequestParam(defaultValue = "1") @Min(1) Integer page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
    return Result.success(governanceUserManagementService.getAdmins(page, size));
  }

  @GetMapping("/api/admins/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<AdminDTO> getAdminById(@PathVariable @NotNull @Positive Long id) {
    return Result.success("Query successful", governanceUserManagementService.getAdminById(id));
  }

  @PostMapping("/api/admins")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<AdminDTO> createAdmin(@RequestBody @Validated AdminUpsertRequestDTO requestDTO) {
    return Result.success("Admin created", governanceUserManagementService.createAdmin(requestDTO));
  }

  @PutMapping("/api/admins/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> updateAdmin(
      @PathVariable @NotNull @Positive Long id,
      @RequestBody @Validated AdminUpsertRequestDTO requestDTO) {
    return Result.success(
        "Admin updated", governanceUserManagementService.updateAdmin(id, requestDTO));
  }

  @DeleteMapping("/api/admins/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> deleteAdmin(@PathVariable @NotNull @Positive Long id) {
    return Result.success("Deleted successfully", governanceUserManagementService.deleteAdmin(id));
  }

  @PatchMapping("/api/admins/{id}/status")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> updateAdminStatus(
      @PathVariable @NotNull @Positive Long id, @RequestParam Integer status) {
    return Result.success(
        "Status updated", governanceUserManagementService.updateAdminStatus(id, status));
  }

  @PostMapping("/api/admins/{id}/password-resets")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<String> resetPassword(@PathVariable @NotNull @Positive Long id) {
    return Result.success(
        "Password reset successful", governanceUserManagementService.resetPassword(id));
  }

  @GetMapping("/api/admin/users")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<UserPageVO> searchUsers(
      @RequestParam(defaultValue = "1") @Min(1) Integer page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String phone,
      @RequestParam(required = false) String nickname,
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) String roleCode) {
    return Result.success(
        governanceUserManagementService.searchUsers(
            page, size, username, email, phone, nickname, status, roleCode));
  }

  @PutMapping("/api/admin/users/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> updateUser(
      @PathVariable @NotNull @Positive Long id,
      @RequestBody @Validated UserUpsertRequestDTO requestDTO) {
    return Result.success(
        "user updated", governanceUserManagementService.updateUser(id, requestDTO));
  }

  @DeleteMapping("/api/admin/users/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> deleteUser(@PathVariable @NotNull @Positive Long id) {
    return Result.success("user deleted", governanceUserManagementService.deleteUser(id));
  }

  @DeleteMapping("/api/admin/users/batch")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> deleteUsers(@RequestBody @NotNull List<Long> ids) {
    return Result.success(
        String.format("batch delete completed: %d", ids.size()),
        governanceUserManagementService.deleteUsers(ids));
  }

  @PutMapping("/api/admin/users/batch")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> updateUsersBatch(
      @RequestBody @Validated @NotNull List<UserUpsertRequestDTO> requestDTOList) {
    return Result.success(
        String.format("batch update completed: %d", requestDTOList.size()),
        governanceUserManagementService.updateUsersBatch(requestDTOList));
  }

  @PatchMapping("/api/admin/users/status/batch")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> updateUserStatusBatch(
      @RequestParam List<Long> ids, @RequestParam Integer status) {
    int successCount = governanceUserManagementService.updateUserStatusBatch(ids, status);
    return Result.success(
        String.format("batch status update completed: %d/%d", successCount, ids.size()), true);
  }

  @GetMapping("/auth/authorizations/statistics")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getTokenStats() {
    return authGovernanceManagementService.getTokenStats();
  }

  @GetMapping("/auth/authorizations/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getAuthorizationDetails(@PathVariable @NotBlank String id) {
    return authGovernanceManagementService.getAuthorizationDetails(id);
  }

  @DeleteMapping("/auth/authorizations/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Void> revokeAuthorization(@PathVariable @NotBlank String id) {
    return authGovernanceManagementService.revokeAuthorization(id);
  }

  @PostMapping("/auth/cleanups/authorizations")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> cleanupExpiredTokens() {
    return authGovernanceManagementService.cleanupAuthorizations();
  }

  @GetMapping("/auth/authorizations/storage-structure")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getStorageStructure() {
    return authGovernanceManagementService.getAuthorizationStorageStructure();
  }

  @GetMapping("/auth/blacklist-entries/statistics")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<TokenBlacklistStatsVO> getBlacklistStats() {
    return authGovernanceManagementService.getBlacklistStats();
  }

  @PostMapping("/auth/blacklist-entries")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Void> addToBlacklist(
      @RequestParam @NotBlank String tokenValue,
      @RequestParam(defaultValue = "admin_manual") @NotBlank String reason) {
    return authGovernanceManagementService.addToBlacklist(tokenValue, reason);
  }

  @GetMapping("/auth/blacklist-entries/check")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> checkBlacklist(@RequestParam @NotBlank String tokenValue) {
    return authGovernanceManagementService.checkBlacklist(tokenValue);
  }

  @PostMapping("/auth/cleanups/blacklist-entries")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> cleanupBlacklist() {
    return authGovernanceManagementService.cleanupBlacklist();
  }

  @GetMapping("/api/admin/mq/consumers")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<List<Map<String, Object>>> getMqConsumers() {
    return Result.success("query successful", mqGovernanceAggregationService.listConsumers());
  }

  @GetMapping("/api/admin/mq/dead-letters/pending")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<List<Map<String, Object>>> getPendingDeadLetters(
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit) {
    return Result.success(
        "query successful", mqGovernanceAggregationService.listPendingDeadLetters(limit));
  }

  @PostMapping("/api/admin/mq/dead-letters/handle")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> handleDeadLetter(
      @RequestParam @NotBlank String serviceId,
      @RequestParam @NotBlank String topic,
      @RequestParam @NotBlank String msgId) {
    return Result.success(
        "dead letter marked as handled",
        mqGovernanceAggregationService.markDeadLetterHandled(serviceId, topic, msgId));
  }

  @GetMapping("/api/admin/outbox/stats")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<List<Map<String, Object>>> getOutboxStats() {
    return Result.success("query successful", outboxGovernanceAggregationService.getStats());
  }

  @GetMapping("/api/admin/outbox/pending")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<List<Map<String, Object>>> getPendingOutboxEvents(
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit) {
    return Result.success(
        "query successful", outboxGovernanceAggregationService.listPending(limit));
  }

  @GetMapping("/api/admin/outbox/dead")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<List<Map<String, Object>>> getDeadOutboxEvents(
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit) {
    return Result.success("query successful", outboxGovernanceAggregationService.listDead(limit));
  }

  @PostMapping("/api/admin/outbox/requeue")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> requeueOutboxEvent(
      @RequestParam @NotBlank String serviceId, @RequestParam @NotNull @Positive Long id) {
    return Result.success(
        "outbox event requeued", outboxGovernanceAggregationService.requeue(serviceId, id));
  }

  @PostMapping("/api/admin/outbox/requeue-batch")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Integer> requeueOutboxEventsBatch(
      @RequestParam @NotBlank String serviceId,
      @RequestBody @Validated OutboxBatchRequeueRequestDTO requestDTO) {
    Integer requeuedCount =
        outboxGovernanceAggregationService.requeueBatch(serviceId, requestDTO.getIds());
    return Result.success(
        String.format("outbox events requeued: %d", requeuedCount), requeuedCount);
  }

  @GetMapping("/api/admin/observability/grafana")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getGrafanaEntry() {
    return Result.success("query successful", observabilityEntryService.getGrafanaEntry());
  }

  @GetMapping("/api/admin/observability/grafana/open")
  @PreAuthorize("hasAuthority('admin:all')")
  public ResponseEntity<Void> openGrafana(@RequestParam(required = false) String dashboardUid) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(observabilityEntryService.resolveGrafanaUrl(dashboardUid)))
        .build();
  }

  @PostMapping("/api/admin/notifications/welcome/{userId}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> sendWelcomeNotification(@PathVariable @NotNull @Positive Long userId) {
    return Result.success(
        "welcome notification enqueued",
        remoteCallSupport.command(
            "user-service.governance.sendWelcomeNotification",
            () -> userNotificationGovernanceDubboApi.sendWelcomeNotification(userId)));
  }

  @PostMapping("/api/admin/notifications/status-change/{userId}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> sendStatusChangeNotification(
      @PathVariable @NotNull @Positive Long userId,
      @RequestBody @Validated UserNotificationStatusChangeRequestDTO requestDTO) {
    return Result.success(
        "status change notification enqueued",
        remoteCallSupport.command(
            "user-service.governance.sendStatusChangeNotification",
            () ->
                userNotificationGovernanceDubboApi.sendStatusChangeNotification(
                    userId, requestDTO)));
  }

  @PostMapping("/api/admin/notifications/batch")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> sendBatchNotification(
      @RequestBody @Validated UserNotificationBatchRequestDTO requestDTO) {
    return Result.success(
        "batch notification enqueued",
        remoteCallSupport.command(
            "user-service.governance.sendBatchNotification",
            () -> userNotificationGovernanceDubboApi.sendBatchNotification(requestDTO)));
  }

  @PostMapping("/api/admin/notifications/system")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> sendSystemAnnouncement(
      @RequestBody @Validated UserSystemAnnouncementRequestDTO requestDTO) {
    return Result.success(
        "system announcement enqueued",
        remoteCallSupport.command(
            "user-service.governance.sendSystemAnnouncement",
            () -> userNotificationGovernanceDubboApi.sendSystemAnnouncement(requestDTO)));
  }
}
