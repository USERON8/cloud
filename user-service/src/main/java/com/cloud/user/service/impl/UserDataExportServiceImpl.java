package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.UserType;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserDataExportService;
import com.cloud.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataExportServiceImpl implements UserDataExportService {

    private static final int BATCH_SIZE = 1000;

    private final UserMapper userMapper;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Resource
    @Qualifier("userCommonAsyncExecutor")
    private Executor userCommonAsyncExecutor;

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<Long> exportUsersToExcelAsync(OutputStream outputStream) {
        return exportUsersToCsvAsync(outputStream);
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<Long> exportUsersToCsvAsync(OutputStream outputStream) {
        String taskId = UUID.randomUUID().toString();

        return CompletableFuture.supplyAsync(() -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                updateExportTaskStatus(taskId, "RUNNING", 0, 0);

                writer.write("ID,Username,Nickname,Email,Phone,UserType,Status,CreatedAt");
                writer.newLine();

                long totalCount = userMapper.selectCount(null);
                long exportedCount = 0;

                for (long offset = 0; offset < totalCount; offset += BATCH_SIZE) {
                    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                    wrapper.last("LIMIT " + offset + ", " + BATCH_SIZE);
                    List<User> users = userMapper.selectList(wrapper);

                    for (User user : users) {
                        String line = String.format("%d,%s,%s,%s,%s,%s,%d,%s",
                                user.getId(),
                                escapeCsv(user.getUsername()),
                                escapeCsv(user.getNickname()),
                                escapeCsv(user.getEmail()),
                                escapeCsv(user.getPhone()),
                                escapeCsv(user.getUserType()),
                                user.getStatus() == null ? 0 : user.getStatus(),
                                user.getCreatedAt());
                        writer.write(line);
                        writer.newLine();
                        exportedCount++;
                    }

                    writer.flush();
                    updateExportTaskStatus(taskId, "RUNNING", totalCount, exportedCount);
                }

                updateExportTaskStatus(taskId, "COMPLETED", totalCount, exportedCount);
                return exportedCount;
            } catch (Exception e) {
                log.error("Failed to export users to CSV", e);
                updateExportTaskStatus(taskId, "FAILED", 0, 0);
                throw new RuntimeException("failed to export users", e);
            }
        }, userCommonAsyncExecutor);
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<ImportResult> importUsersFromExcelAsync(MultipartFile file) {
        return importUsersFromCsvAsync(file);
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<ImportResult> importUsersFromCsvAsync(MultipartFile file) {
        String taskId = UUID.randomUUID().toString();

        return CompletableFuture.supplyAsync(() -> {
            List<String> errors = new ArrayList<>();
            long successCount = 0;
            long failureCount = 0;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                updateImportTaskStatus(taskId, "RUNNING", 0, 0, 0);

                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IllegalArgumentException("CSV file is empty");
                }

                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    try {
                        String[] fields = line.split(",");
                        if (fields.length < 7) {
                            errors.add("line " + lineNumber + " has invalid field count");
                            failureCount++;
                            continue;
                        }

                        UserDTO userDTO = new UserDTO();
                        userDTO.setUsername(fields[1]);
                        userDTO.setNickname(fields[2]);
                        userDTO.setEmail(fields[3]);
                        userDTO.setPhone(fields[4]);
                        UserType userType = UserType.fromCode(fields[5]);
                        userDTO.setUserType(userType == null ? UserType.USER : userType);
                        userDTO.setStatus(Integer.parseInt(fields[6]));
                        userService.createUser(userDTO);
                        successCount++;
                    } catch (Exception e) {
                        errors.add("line " + lineNumber + " failed: " + e.getMessage());
                        failureCount++;
                        log.warn("Failed to import one CSV line, lineNumber={}", lineNumber, e);
                    }

                    long processed = successCount + failureCount;
                    if (processed % 100 == 0) {
                        updateImportTaskStatus(taskId, "RUNNING", processed, successCount, failureCount);
                    }
                }

                updateImportTaskStatus(taskId, "COMPLETED", successCount + failureCount, successCount, failureCount);
                return new ImportResult(successCount, failureCount, errors);
            } catch (Exception e) {
                log.error("Failed to import users from CSV", e);
                updateImportTaskStatus(taskId, "FAILED", 0, 0, 0);
                errors.add("import failed: " + e.getMessage());
                return new ImportResult(successCount, failureCount, errors);
            }
        }, userCommonAsyncExecutor);
    }

    @Override
    public CompletableFuture<ExportTaskStatus> getExportTaskStatus(String taskId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = "export:task:" + taskId;
                Object status = redisTemplate.opsForHash().get(key, "status");
                Object total = redisTemplate.opsForHash().get(key, "total");
                Object exported = redisTemplate.opsForHash().get(key, "exported");
                Object filePath = redisTemplate.opsForHash().get(key, "filePath");
                return new ExportTaskStatus(
                        taskId,
                        status == null ? "UNKNOWN" : status.toString(),
                        total == null ? 0 : Long.parseLong(total.toString()),
                        exported == null ? 0 : Long.parseLong(exported.toString()),
                        filePath == null ? null : filePath.toString()
                );
            } catch (Exception e) {
                log.error("Failed to read export task status", e);
                return new ExportTaskStatus(taskId, "ERROR", 0, 0, null);
            }
        }, userCommonAsyncExecutor);
    }

    @Override
    public CompletableFuture<ImportTaskStatus> getImportTaskStatus(String taskId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = "import:task:" + taskId;
                Object status = redisTemplate.opsForHash().get(key, "status");
                Object total = redisTemplate.opsForHash().get(key, "total");
                Object imported = redisTemplate.opsForHash().get(key, "imported");
                Object failed = redisTemplate.opsForHash().get(key, "failed");
                return new ImportTaskStatus(
                        taskId,
                        status == null ? "UNKNOWN" : status.toString(),
                        total == null ? 0 : Long.parseLong(total.toString()),
                        imported == null ? 0 : Long.parseLong(imported.toString()),
                        failed == null ? 0 : Long.parseLong(failed.toString())
                );
            } catch (Exception e) {
                log.error("Failed to read import task status", e);
                return new ImportTaskStatus(taskId, "ERROR", 0, 0, 0);
            }
        }, userCommonAsyncExecutor);
    }

    private void updateExportTaskStatus(String taskId, String status, long total, long exported) {
        try {
            String key = "export:task:" + taskId;
            redisTemplate.opsForHash().put(key, "status", status);
            redisTemplate.opsForHash().put(key, "total", total);
            redisTemplate.opsForHash().put(key, "exported", exported);
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to update export task status, taskId={}", taskId, e);
        }
    }

    private void updateImportTaskStatus(String taskId, String status, long total, long imported, long failed) {
        try {
            String key = "import:task:" + taskId;
            redisTemplate.opsForHash().put(key, "status", status);
            redisTemplate.opsForHash().put(key, "total", total);
            redisTemplate.opsForHash().put(key, "imported", imported);
            redisTemplate.opsForHash().put(key, "failed", failed);
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to update import task status, taskId={}", taskId, e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}