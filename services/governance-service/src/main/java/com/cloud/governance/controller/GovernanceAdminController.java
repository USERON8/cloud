package com.cloud.governance.controller;

import com.cloud.api.auth.AuthGovernanceDubboApi;
import com.cloud.api.user.AdminGovernanceDubboApi;
import com.cloud.api.user.UserAdminGovernanceDubboApi;
import com.cloud.api.user.UserGovernanceDubboApi;
import com.cloud.api.user.UserNotificationGovernanceDubboApi;
import com.cloud.common.domain.dto.governance.OutboxBatchRequeueRequestDTO;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.common.domain.dto.user.UserNotificationBatchRequestDTO;
import com.cloud.common.domain.dto.user.UserNotificationStatusChangeRequestDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.dto.user.UserSystemAnnouncementRequestDTO;
import com.cloud.common.domain.dto.user.UserUpsertRequestDTO;
import com.cloud.common.domain.support.AuthGovernancePayloadMapper;
import com.cloud.common.domain.vo.auth.AuthAuthorizationDetailVO;
import com.cloud.common.domain.vo.auth.AuthTokenStorageStatsVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistCheckVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistStatsVO;
import com.cloud.common.domain.vo.governance.ThreadPoolMetricsVO;
import com.cloud.common.domain.vo.user.AdminPageVO;
import com.cloud.common.domain.vo.user.UserPageVO;
import com.cloud.common.domain.vo.user.UserStatisticsVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.remote.RemoteCallSupport;
import com.cloud.common.result.Result;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
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
@Tag(name = "Governance Admin API", description = "Governance-owned admin APIs")
public class GovernanceAdminController {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserGovernanceDubboApi userGovernanceDubboApi;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private AdminGovernanceDubboApi adminGovernanceDubboApi;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserAdminGovernanceDubboApi userAdminGovernanceDubboApi;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private AuthGovernanceDubboApi authGovernanceDubboApi;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserNotificationGovernanceDubboApi userNotificationGovernanceDubboApi;

  private final RemoteCallSupport remoteCallSupport;
  private final MqGovernanceAggregationService mqGovernanceAggregationService;
  private final OutboxGovernanceAggregationService outboxGovernanceAggregationService;
  private final ObservabilityEntryService observabilityEntryService;

  @GetMapping("/api/admin/statistics/overview")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Get statistics overview through governance-service")
  public Result<UserStatisticsVO> getStatisticsOverview() {
    return Result.success(
        "query successful",
        remoteCallSupport.query(
            "user-service.governance.getStatisticsOverview",
            userGovernanceDubboApi::getStatisticsOverview));
  }

  @GetMapping("/api/admin/statistics/overview/async")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Get statistics overview async through governance-service")
  public CompletableFuture<Result<UserStatisticsVO>> getStatisticsOverviewAsync() {
    return CompletableFuture.completedFuture(getStatisticsOverview());
  }

  @GetMapping("/api/admin/statistics/registration-trend")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Get registration trend through governance-service")
  public Result<Map<LocalDate, Long>> getRegistrationTrend(
      @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate) {
    validateDateRange(startDate, endDate);
    return Result.success(
        "query successful",
        remoteCallSupport.query(
            "user-service.governance.getRegistrationTrend",
            () -> userGovernanceDubboApi.getRegistrationTrend(startDate, endDate)));
  }

  @GetMapping("/api/admin/statistics/registration-trend/async")
  @PreAuthorize("hasAuthority('admin:all')")
  @Operation(summary = "Get registration trend async through governance-service")
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
            .map(this::toThreadPoolMap)
            .toList();
    return Result.success(items);
  }

  @GetMapping("/api/admin/thread-pools/{name}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getThreadPoolInfoByName(
      @PathVariable @Parameter(description = "Thread pool bean name") String name) {
    ThreadPoolMetricsVO metrics =
        remoteCallSupport.query(
            "user-service.governance.getThreadPoolInfo",
            () -> userGovernanceDubboApi.getThreadPoolInfo(name));
    if (metrics == null) {
      throw new BizException(ResultCode.NOT_FOUND, "Thread pool bean not found: " + name);
    }
    return Result.success(toThreadPoolMap(metrics));
  }

  @GetMapping("/api/admins")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<AdminPageVO> getAdmins(
      @RequestParam(defaultValue = "1") @Min(1) Integer page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.getAdminsPage",
            () -> adminGovernanceDubboApi.getAdminsPage(page, size)));
  }

  @GetMapping("/api/admins/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<AdminDTO> getAdminById(@PathVariable @NotNull @Positive Long id) {
    return Result.success(
        "Query successful",
        remoteCallSupport.query(
            "user-service.governance.getAdminById",
            () -> adminGovernanceDubboApi.getAdminById(id)));
  }

  @PostMapping("/api/admins")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<AdminDTO> createAdmin(@RequestBody @Validated AdminUpsertRequestDTO requestDTO) {
    return Result.success(
        "Admin created",
        remoteCallSupport.command(
            "user-service.governance.createAdmin",
            () -> adminGovernanceDubboApi.createAdmin(requestDTO)));
  }

  @PutMapping("/api/admins/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> updateAdmin(
      @PathVariable @NotNull @Positive Long id,
      @RequestBody @Validated AdminUpsertRequestDTO requestDTO) {
    return Result.success(
        "Admin updated",
        remoteCallSupport.command(
            "user-service.governance.updateAdmin",
            () -> adminGovernanceDubboApi.updateAdmin(id, requestDTO)));
  }

  @DeleteMapping("/api/admins/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> deleteAdmin(@PathVariable @NotNull @Positive Long id) {
    return Result.success(
        "Deleted successfully",
        remoteCallSupport.command(
            "user-service.governance.deleteAdmin", () -> adminGovernanceDubboApi.deleteAdmin(id)));
  }

  @PatchMapping("/api/admins/{id}/status")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> updateAdminStatus(
      @PathVariable @NotNull @Positive Long id, @RequestParam Integer status) {
    return Result.success(
        "Status updated",
        remoteCallSupport.command(
            "user-service.governance.updateAdminStatus",
            () -> adminGovernanceDubboApi.updateAdminStatus(id, status)));
  }

  @PostMapping("/api/admins/{id}/password-resets")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<String> resetPassword(@PathVariable @NotNull @Positive Long id) {
    return Result.success(
        "Password reset successful",
        remoteCallSupport.command(
            "user-service.governance.resetPassword",
            () -> adminGovernanceDubboApi.resetPassword(id)));
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
    UserPageDTO request = new UserPageDTO();
    request.setCurrent(page.longValue());
    request.setSize(size.longValue());
    request.setUsername(username);
    request.setEmail(email);
    request.setPhone(phone);
    request.setNickname(nickname);
    request.setStatus(status);
    request.setRoleCode(roleCode);
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.searchUsers",
            () -> userAdminGovernanceDubboApi.searchUsers(request)));
  }

  @PutMapping("/api/admin/users/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> updateUser(
      @PathVariable @NotNull @Positive Long id,
      @RequestBody @Validated UserUpsertRequestDTO requestDTO) {
    return Result.success(
        "user updated",
        remoteCallSupport.command(
            "user-service.governance.updateUser",
            () -> userAdminGovernanceDubboApi.updateUser(id, requestDTO)));
  }

  @DeleteMapping("/api/admin/users/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> deleteUser(@PathVariable @NotNull @Positive Long id) {
    return Result.success(
        "user deleted",
        remoteCallSupport.command(
            "user-service.governance.deleteUser",
            () -> userAdminGovernanceDubboApi.deleteUser(id)));
  }

  @DeleteMapping("/api/admin/users/batch")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> deleteUsers(@RequestBody @NotNull List<Long> ids) {
    return Result.success(
        String.format("batch delete completed: %d", ids.size()),
        remoteCallSupport.command(
            "user-service.governance.deleteUsers",
            () -> userAdminGovernanceDubboApi.deleteUsers(ids)));
  }

  @PutMapping("/api/admin/users/batch")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> updateUsersBatch(
      @RequestBody @Validated @NotNull List<UserUpsertRequestDTO> requestDTOList) {
    return Result.success(
        String.format("batch update completed: %d", requestDTOList.size()),
        remoteCallSupport.command(
            "user-service.governance.updateUsersBatch",
            () -> userAdminGovernanceDubboApi.updateUsersBatch(requestDTOList)));
  }

  @PatchMapping("/api/admin/users/status/batch")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Boolean> updateUserStatusBatch(
      @RequestParam List<Long> ids, @RequestParam Integer status) {
    Integer successCount =
        remoteCallSupport.command(
            "user-service.governance.updateUserStatusBatch",
            () -> userAdminGovernanceDubboApi.updateUserStatusBatch(ids, status));
    if (successCount == null) {
      successCount = 0;
    }
    return Result.success(
        String.format("batch status update completed: %d/%d", successCount, ids.size()), true);
  }

  @GetMapping("/auth/authorizations/statistics")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getTokenStats() {
    AuthTokenStorageStatsVO stats =
        remoteCallSupport.query(
            "auth-service.governance.getTokenStats", authGovernanceDubboApi::getTokenStats);
    Map<String, Object> payload = new HashMap<>();
    payload.put("authorizationCount", stats.getAuthorizationCount());
    payload.put("accessIndexCount", stats.getAccessIndexCount());
    payload.put("refreshIndexCount", stats.getRefreshIndexCount());
    payload.put("codeIndexCount", stats.getCodeIndexCount());
    payload.put("principalIndexCount", stats.getPrincipalIndexCount());
    payload.put("redisInfo", stats.getRedisInfo());
    payload.put("storageType", stats.getStorageType());
    return Result.success(payload);
  }

  @GetMapping("/auth/authorizations/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getAuthorizationDetails(@PathVariable @NotBlank String id) {
    AuthAuthorizationDetailVO detail =
        remoteCallSupport.query(
            "auth-service.governance.getAuthorizationDetails",
            () -> authGovernanceDubboApi.getAuthorizationDetails(id));
    return Result.success(AuthGovernancePayloadMapper.toAuthorizationDetailPayload(detail));
  }

  @DeleteMapping("/auth/authorizations/{id}")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Void> revokeAuthorization(@PathVariable @NotBlank String id) {
    remoteCallSupport.command(
        "auth-service.governance.revokeAuthorization",
        () -> authGovernanceDubboApi.revokeAuthorization(id));
    return Result.success();
  }

  @PostMapping("/auth/cleanups/authorizations")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> cleanupExpiredTokens() {
    return Result.success(
        remoteCallSupport.command(
            "auth-service.governance.cleanupAuthorizations",
            authGovernanceDubboApi::cleanupAuthorizations));
  }

  @GetMapping("/auth/authorizations/storage-structure")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> getStorageStructure() {
    return Result.success(
        remoteCallSupport.query(
            "auth-service.governance.getAuthorizationStorageStructure",
            authGovernanceDubboApi::getAuthorizationStorageStructure));
  }

  @GetMapping("/auth/blacklist-entries/statistics")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<TokenBlacklistStatsVO> getBlacklistStats() {
    return Result.success(
        remoteCallSupport.query(
            "auth-service.governance.getBlacklistStats",
            authGovernanceDubboApi::getBlacklistStats));
  }

  @PostMapping("/auth/blacklist-entries")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Void> addToBlacklist(
      @RequestParam @NotBlank String tokenValue,
      @RequestParam(defaultValue = "admin_manual") @NotBlank String reason) {
    remoteCallSupport.command(
        "auth-service.governance.addToBlacklist",
        () -> authGovernanceDubboApi.addToBlacklist(tokenValue, reason));
    return Result.success();
  }

  @GetMapping("/auth/blacklist-entries/check")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> checkBlacklist(@RequestParam @NotBlank String tokenValue) {
    TokenBlacklistCheckVO result =
        remoteCallSupport.query(
            "auth-service.governance.checkBlacklist",
            () -> authGovernanceDubboApi.checkBlacklist(tokenValue));
    Map<String, Object> payload = new HashMap<>();
    payload.put("tokenValue", result.getTokenPreview());
    payload.put("isBlacklisted", result.getBlacklisted());
    payload.put("checkTime", result.getCheckedAt());
    return Result.success(payload);
  }

  @PostMapping("/auth/cleanups/blacklist-entries")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Object>> cleanupBlacklist() {
    Integer cleanedCount =
        remoteCallSupport.command(
            "auth-service.governance.cleanupBlacklist", authGovernanceDubboApi::cleanupBlacklist);
    Map<String, Object> payload = new HashMap<>();
    payload.put("cleanedCount", cleanedCount);
    payload.put("message", "Blacklist cleanup completed");
    payload.put("cleanupTime", Instant.now());
    return Result.success(payload);
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

  private void validateDateRange(LocalDate startDate, LocalDate endDate) {
    if (endDate.isBefore(startDate)) {
      throw new BizException(
          ResultCode.BAD_REQUEST, "endDate must be greater than or equal to startDate");
    }
    if (ChronoUnit.DAYS.between(startDate, endDate) > 365) {
      throw new BizException(ResultCode.BAD_REQUEST, "date range cannot exceed 365 days");
    }
  }

  private Map<String, Object> toThreadPoolMap(ThreadPoolMetricsVO metrics) {
    Map<String, Object> item = new HashMap<>();
    item.put("name", metrics.getName());
    item.put("corePoolSize", metrics.getCorePoolSize());
    item.put("maxPoolSize", metrics.getMaxPoolSize());
    item.put("activeCount", metrics.getActiveCount());
    item.put("poolSize", metrics.getPoolSize());
    item.put("queueSize", metrics.getQueueSize());
    item.put("completedTaskCount", metrics.getCompletedTaskCount());
    item.put("taskCount", metrics.getTaskCount());
    item.put("queueRemainingCapacity", metrics.getQueueRemainingCapacity());
    return item;
  }
}
