package com.cloud.governance.controller;

import com.cloud.api.auth.AuthGovernanceDubboApi;
import com.cloud.api.stock.StockDubboApi;
import com.cloud.api.user.AdminGovernanceDubboApi;
import com.cloud.api.user.UserAdminGovernanceDubboApi;
import com.cloud.api.user.UserGovernanceDubboApi;
import com.cloud.api.user.UserNotificationGovernanceDubboApi;
import com.cloud.common.domain.dto.governance.OutboxBatchRequeueRequestDTO;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.common.domain.dto.user.AdminUpsertRequestDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserNotificationBatchRequestDTO;
import com.cloud.common.domain.dto.user.UserNotificationStatusChangeRequestDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.dto.user.UserSystemAnnouncementRequestDTO;
import com.cloud.common.domain.dto.user.UserUpsertRequestDTO;
import com.cloud.common.domain.vo.auth.AuthAuthorizationDetailVO;
import com.cloud.common.domain.vo.auth.AuthTokenStorageStatsVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistCheckVO;
import com.cloud.common.domain.vo.auth.TokenBlacklistStatsVO;
import com.cloud.common.domain.vo.governance.ThreadPoolMetricsVO;
import com.cloud.common.domain.vo.stock.StockLedgerVO;
import com.cloud.common.domain.vo.user.AdminPageVO;
import com.cloud.common.domain.vo.user.UserPageVO;
import com.cloud.common.domain.vo.user.UserStatisticsVO;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/governance")
@RequiredArgsConstructor
@Validated
@Tag(name = "Governance API", description = "Internal governance aggregation APIs")
public class GovernanceController {

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private AuthGovernanceDubboApi authGovernanceDubboApi;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private AdminGovernanceDubboApi adminGovernanceDubboApi;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserGovernanceDubboApi userGovernanceDubboApi;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserAdminGovernanceDubboApi userAdminGovernanceDubboApi;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserNotificationGovernanceDubboApi userNotificationGovernanceDubboApi;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private StockDubboApi stockDubboApi;

  private final RemoteCallSupport remoteCallSupport;
  private final MqGovernanceAggregationService mqGovernanceAggregationService;
  private final OutboxGovernanceAggregationService outboxGovernanceAggregationService;
  private final ObservabilityEntryService observabilityEntryService;

  @GetMapping("/users/statistics/overview")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get user statistics overview")
  public Result<UserStatisticsVO> getUserStatisticsOverview() {
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.getStatisticsOverview",
            userGovernanceDubboApi::getStatisticsOverview));
  }

  @GetMapping("/users/statistics/registration-trend")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get user registration trend")
  public Result<Map<LocalDate, Long>> getRegistrationTrend(
      @RequestParam @Parameter(description = "Start date") @DateTimeFormat(iso = ISO.DATE)
          LocalDate startDate,
      @RequestParam @Parameter(description = "End date") @DateTimeFormat(iso = ISO.DATE)
          LocalDate endDate) {
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.getRegistrationTrend",
            () -> userGovernanceDubboApi.getRegistrationTrend(startDate, endDate)));
  }

  @GetMapping("/users/statistics/role-distribution")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get user role distribution")
  public Result<Map<String, Long>> getRoleDistribution() {
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.getRoleDistribution",
            userGovernanceDubboApi::getRoleDistribution));
  }

  @GetMapping("/users/statistics/status-distribution")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get user status distribution")
  public Result<Map<String, Long>> getStatusDistribution() {
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.getStatusDistribution",
            userGovernanceDubboApi::getStatusDistribution));
  }

  @GetMapping("/users/statistics/active-users")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Count active users")
  public Result<Long> countActiveUsers(
      @RequestParam(defaultValue = "7")
          @Parameter(description = "Recent days")
          @Min(value = 1)
          @Max(value = 365)
          Integer days) {
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.countActiveUsers",
            () -> userGovernanceDubboApi.countActiveUsers(days)));
  }

  @GetMapping("/users/statistics/growth-rate")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Calculate user growth rate")
  public Result<Double> calculateGrowthRate(
      @RequestParam(defaultValue = "7")
          @Parameter(description = "Recent days")
          @Min(value = 1)
          @Max(value = 365)
          Integer days) {
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.calculateGrowthRate",
            () -> userGovernanceDubboApi.calculateGrowthRate(days)));
  }

  @GetMapping("/users/statistics/activity-ranking")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get user activity ranking")
  public Result<Map<Long, Long>> getActivityRanking(
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer limit,
      @RequestParam(defaultValue = "30") @Min(1) @Max(365) Integer days) {
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.getActivityRanking",
            () -> userGovernanceDubboApi.getActivityRanking(limit, days)));
  }

  @PostMapping("/users/statistics/refresh-cache")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Refresh user statistics cache")
  public Result<Boolean> refreshStatisticsCache() {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.refreshStatisticsCache",
            userGovernanceDubboApi::refreshStatisticsCache));
  }

  @GetMapping("/thread-pools")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get thread pool metrics")
  public Result<List<ThreadPoolMetricsVO>> getThreadPoolInfoList() {
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.getThreadPoolInfoList",
            userGovernanceDubboApi::getThreadPoolInfoList));
  }

  @GetMapping("/thread-pools/{name}")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get thread pool metrics by name")
  public Result<ThreadPoolMetricsVO> getThreadPoolInfo(@PathVariable String name) {
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.getThreadPoolInfo",
            () -> userGovernanceDubboApi.getThreadPoolInfo(name)));
  }

  @GetMapping("/stocks/ledger/{skuId}")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get stock ledger by sku")
  public Result<StockLedgerVO> getStockLedger(@PathVariable Long skuId) {
    return Result.success(
        remoteCallSupport.query(
            "stock-service.governance.getLedgerBySkuId",
            () -> stockDubboApi.getLedgerBySkuId(skuId)));
  }

  @GetMapping("/admins")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get admins with pagination")
  public Result<AdminPageVO> getAdmins(
      @RequestParam(defaultValue = "1") @Min(1) Integer page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.getAdminsPage",
            () -> adminGovernanceDubboApi.getAdminsPage(page, size)));
  }

  @GetMapping("/admins/{id}")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get admin details")
  public Result<AdminDTO> getAdminById(@PathVariable @NotNull @Positive Long id) {
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.getAdminById",
            () -> adminGovernanceDubboApi.getAdminById(id)));
  }

  @PostMapping("/admins")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Create admin")
  public Result<AdminDTO> createAdmin(@RequestBody @Validated AdminUpsertRequestDTO requestDTO) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.createAdmin",
            () -> adminGovernanceDubboApi.createAdmin(requestDTO)));
  }

  @PutMapping("/admins/{id}")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Update admin")
  public Result<Boolean> updateAdmin(
      @PathVariable @NotNull @Positive Long id,
      @RequestBody @Validated AdminUpsertRequestDTO requestDTO) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.updateAdmin",
            () -> adminGovernanceDubboApi.updateAdmin(id, requestDTO)));
  }

  @DeleteMapping("/admins/{id}")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Delete admin")
  public Result<Boolean> deleteAdmin(@PathVariable @NotNull @Positive Long id) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.deleteAdmin", () -> adminGovernanceDubboApi.deleteAdmin(id)));
  }

  @PatchMapping("/admins/{id}/status")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Update admin status")
  public Result<Boolean> updateAdminStatus(
      @PathVariable @NotNull @Positive Long id, @RequestParam Integer status) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.updateAdminStatus",
            () -> adminGovernanceDubboApi.updateAdminStatus(id, status)));
  }

  @PostMapping("/admins/{id}/reset-password")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Reset admin password")
  public Result<String> resetPassword(@PathVariable @NotNull @Positive Long id) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.resetPassword",
            () -> adminGovernanceDubboApi.resetPassword(id)));
  }

  @GetMapping("/users/query")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Find user by username")
  public Result<UserDTO> findUserByUsername(@RequestParam @NotBlank String username) {
    return Result.success(
        remoteCallSupport.query(
            "user-service.governance.findByUsername",
            () -> userAdminGovernanceDubboApi.findByUsername(username)));
  }

  @GetMapping("/users/search")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Search users with pagination")
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

  @PutMapping("/users/{id}")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Update user")
  public Result<Boolean> updateUser(
      @PathVariable @NotNull @Positive Long id,
      @RequestBody @Validated UserUpsertRequestDTO requestDTO) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.updateUser",
            () -> userAdminGovernanceDubboApi.updateUser(id, requestDTO)));
  }

  @DeleteMapping("/users/{id}")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Delete user")
  public Result<Boolean> deleteUser(@PathVariable @NotNull @Positive Long id) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.deleteUser",
            () -> userAdminGovernanceDubboApi.deleteUser(id)));
  }

  @PostMapping("/users/delete-batch")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Delete users in batch")
  public Result<Boolean> deleteUsers(@RequestBody @Validated List<Long> ids) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.deleteUsers",
            () -> userAdminGovernanceDubboApi.deleteUsers(ids)));
  }

  @PostMapping("/users/update-batch")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Update users in batch")
  public Result<Boolean> updateUsersBatch(
      @RequestBody @Validated List<UserUpsertRequestDTO> requestDTOList) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.updateUsersBatch",
            () -> userAdminGovernanceDubboApi.updateUsersBatch(requestDTOList)));
  }

  @PostMapping("/users/status-batch")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Update user status in batch")
  public Result<Integer> updateUserStatusBatch(
      @RequestParam List<Long> ids, @RequestParam Integer status) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.updateUserStatusBatch",
            () -> userAdminGovernanceDubboApi.updateUserStatusBatch(ids, status)));
  }

  @GetMapping("/auth/tokens/stats")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get token storage statistics")
  public Result<AuthTokenStorageStatsVO> getTokenStats() {
    return Result.success(
        remoteCallSupport.query(
            "auth-service.governance.getTokenStats", authGovernanceDubboApi::getTokenStats));
  }

  @GetMapping("/auth/tokens/authorization/{id}")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get authorization details")
  public Result<AuthAuthorizationDetailVO> getAuthorizationDetails(@PathVariable String id) {
    return Result.success(
        remoteCallSupport.query(
            "auth-service.governance.getAuthorizationDetails",
            () -> authGovernanceDubboApi.getAuthorizationDetails(id)));
  }

  @PostMapping("/auth/tokens/authorization/{id}/revoke")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Revoke authorization")
  public Result<Boolean> revokeAuthorization(@PathVariable String id) {
    return Result.success(
        remoteCallSupport.command(
            "auth-service.governance.revokeAuthorization",
            () -> authGovernanceDubboApi.revokeAuthorization(id)));
  }

  @GetMapping("/auth/tokens/blacklist/stats")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get blacklist statistics")
  public Result<TokenBlacklistStatsVO> getBlacklistStats() {
    return Result.success(
        remoteCallSupport.query(
            "auth-service.governance.getBlacklistStats",
            authGovernanceDubboApi::getBlacklistStats));
  }

  @PostMapping("/auth/tokens/blacklist/add")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Add token to blacklist")
  public Result<Boolean> addToBlacklist(
      @RequestParam @Parameter(description = "Token value") String tokenValue,
      @RequestParam(defaultValue = "governance_manual")
          @Parameter(description = "Revocation reason")
          String reason) {
    return Result.success(
        remoteCallSupport.command(
            "auth-service.governance.addToBlacklist",
            () -> authGovernanceDubboApi.addToBlacklist(tokenValue, reason)));
  }

  @GetMapping("/auth/tokens/blacklist/check")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Check blacklist status")
  public Result<TokenBlacklistCheckVO> checkBlacklist(
      @RequestParam @Parameter(description = "Token value") String tokenValue) {
    return Result.success(
        remoteCallSupport.query(
            "auth-service.governance.checkBlacklist",
            () -> authGovernanceDubboApi.checkBlacklist(tokenValue)));
  }

  @PostMapping("/auth/tokens/blacklist/cleanup")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Cleanup blacklist entries")
  public Result<Integer> cleanupBlacklist() {
    return Result.success(
        remoteCallSupport.command(
            "auth-service.governance.cleanupBlacklist", authGovernanceDubboApi::cleanupBlacklist));
  }

  @GetMapping("/mq/consumers")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "List MQ consumers across business services")
  public Result<List<Map<String, Object>>> getMqConsumers() {
    return Result.success(mqGovernanceAggregationService.listConsumers());
  }

  @GetMapping("/mq/dead-letters/pending")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "List pending dead letters across business services")
  public Result<List<Map<String, Object>>> getPendingDeadLetters(
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit) {
    return Result.success(mqGovernanceAggregationService.listPendingDeadLetters(limit));
  }

  @PostMapping("/mq/dead-letters/handle")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Mark a dead letter as handled in one business service")
  public Result<Boolean> handleDeadLetter(
      @RequestParam @NotBlank String serviceId,
      @RequestParam @NotBlank String topic,
      @RequestParam @NotBlank String msgId) {
    return Result.success(
        mqGovernanceAggregationService.markDeadLetterHandled(serviceId, topic, msgId));
  }

  @GetMapping("/outbox/stats")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get outbox stats across business services")
  public Result<List<Map<String, Object>>> getOutboxStats() {
    return Result.success(outboxGovernanceAggregationService.getStats());
  }

  @GetMapping("/outbox/pending")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "List pending outbox events across business services")
  public Result<List<Map<String, Object>>> getPendingOutboxEvents(
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit) {
    return Result.success(outboxGovernanceAggregationService.listPending(limit));
  }

  @GetMapping("/outbox/dead")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "List dead outbox events across business services")
  public Result<List<Map<String, Object>>> getDeadOutboxEvents(
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit) {
    return Result.success(outboxGovernanceAggregationService.listDead(limit));
  }

  @PostMapping("/outbox/requeue")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Requeue one outbox event in one business service")
  public Result<Boolean> requeueOutboxEvent(
      @RequestParam @NotBlank String serviceId, @RequestParam @NotNull @Positive Long id) {
    return Result.success(outboxGovernanceAggregationService.requeue(serviceId, id));
  }

  @PostMapping("/outbox/requeue-batch")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Requeue outbox events in batch for one business service")
  public Result<Integer> requeueOutboxEventsBatch(
      @RequestParam @NotBlank String serviceId,
      @RequestBody @Validated OutboxBatchRequeueRequestDTO requestDTO) {
    return Result.success(
        outboxGovernanceAggregationService.requeueBatch(serviceId, requestDTO.getIds()));
  }

  @GetMapping("/observability/grafana")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Get Grafana observability entry metadata")
  public Result<Map<String, Object>> getGrafanaEntry() {
    return Result.success(observabilityEntryService.getGrafanaEntry());
  }

  @GetMapping("/observability/grafana/open")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Open Grafana through governance-service controlled redirect")
  public ResponseEntity<Void> openGrafana(@RequestParam(required = false) String dashboardUid) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(observabilityEntryService.resolveGrafanaUrl(dashboardUid)))
        .build();
  }

  @PostMapping("/notifications/welcome/{userId}")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Send welcome notification through governance-service")
  public Result<Boolean> sendWelcomeNotification(@PathVariable @NotNull @Positive Long userId) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.sendWelcomeNotification",
            () -> userNotificationGovernanceDubboApi.sendWelcomeNotification(userId)));
  }

  @PostMapping("/notifications/status-change/{userId}")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Send status change notification through governance-service")
  public Result<Boolean> sendStatusChangeNotification(
      @PathVariable @NotNull @Positive Long userId,
      @RequestBody @Validated UserNotificationStatusChangeRequestDTO requestDTO) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.sendStatusChangeNotification",
            () ->
                userNotificationGovernanceDubboApi.sendStatusChangeNotification(
                    userId, requestDTO)));
  }

  @PostMapping("/notifications/batch")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Send batch notification through governance-service")
  public Result<Boolean> sendBatchNotification(
      @RequestBody @Validated UserNotificationBatchRequestDTO requestDTO) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.sendBatchNotification",
            () -> userNotificationGovernanceDubboApi.sendBatchNotification(requestDTO)));
  }

  @PostMapping("/notifications/system")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  @Operation(summary = "Send system announcement through governance-service")
  public Result<Boolean> sendSystemAnnouncement(
      @RequestBody @Validated UserSystemAnnouncementRequestDTO requestDTO) {
    return Result.success(
        remoteCallSupport.command(
            "user-service.governance.sendSystemAnnouncement",
            () -> userNotificationGovernanceDubboApi.sendSystemAnnouncement(requestDTO)));
  }
}
