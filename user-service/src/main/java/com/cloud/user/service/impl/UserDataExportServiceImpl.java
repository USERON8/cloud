package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserDataExportService;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.Resource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 用户数据导入导出服务实现
 * 支持大批量数据的异步处理
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataExportServiceImpl implements UserDataExportService {

    /**
     * 批处理大小
     */
    private static final int BATCH_SIZE = 1000;
    private final UserMapper userMapper;
    private final UserService userService;
    private final UserConverter userConverter;
    private final RedisTemplate<String, Object> redisTemplate;
    @Resource
    @Qualifier("userCommonAsyncExecutor")
    private Executor userCommonAsyncExecutor;

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<Long> exportUsersToExcelAsync(OutputStream outputStream) {
        log.info("开始异步导出用户数据到Excel");
        String taskId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 更新任务状态
                updateExportTaskStatus(taskId, "RUNNING", 0, 0);

                // 查询总数
                long totalCount = userMapper.selectCount(null);
                log.info("待导出用户总数: {}", totalCount);

                // TODO: 集成POI导出Excel
                // 这里仅作为示例，实际需要使用Apache POI
                long exportedCount = 0;

                // 分批查询并导出
                for (long offset = 0; offset < totalCount; offset += BATCH_SIZE) {
                    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                    wrapper.last("LIMIT " + offset + ", " + BATCH_SIZE);

                    List<User> users = userMapper.selectList(wrapper);
                    // 写入Excel
                    exportedCount += users.size();

                    // 更新进度
                    updateExportTaskStatus(taskId, "RUNNING", totalCount, exportedCount);
                }

                // 完成
                updateExportTaskStatus(taskId, "COMPLETED", totalCount, exportedCount);

                log.info("用户数据导出到Excel完成，总数: {}, 耗时: {}ms",
                        exportedCount, System.currentTimeMillis() - startTime);
                return exportedCount;

            } catch (Exception e) {
                log.error("导出用户数据到Excel失败", e);
                updateExportTaskStatus(taskId, "FAILED", 0, 0);
                throw new RuntimeException("导出失败", e);
            }
        }, userCommonAsyncExecutor);
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<Long> exportUsersToCsvAsync(OutputStream outputStream) {
        log.info("开始异步导出用户数据到CSV");
        String taskId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

                updateExportTaskStatus(taskId, "RUNNING", 0, 0);

                // 写入CSV头
                writer.write("ID,Username,Nickname,Email,Phone,UserType,Status,CreatedAt");
                writer.newLine();

                // 查询总数
                long totalCount = userMapper.selectCount(null);
                log.info("待导出用户总数: {}", totalCount);

                long exportedCount = 0;

                // 分批查询并导出
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
                                user.getUserType(),
                                user.getStatus(),
                                user.getCreatedAt());
                        writer.write(line);
                        writer.newLine();
                        exportedCount++;
                    }

                    writer.flush();
                    updateExportTaskStatus(taskId, "RUNNING", totalCount, exportedCount);
                }

                updateExportTaskStatus(taskId, "COMPLETED", totalCount, exportedCount);

                log.info("用户数据导出到CSV完成，总数: {}, 耗时: {}ms",
                        exportedCount, System.currentTimeMillis() - startTime);
                return exportedCount;

            } catch (Exception e) {
                log.error("导出用户数据到CSV失败", e);
                updateExportTaskStatus(taskId, "FAILED", 0, 0);
                throw new RuntimeException("导出失败", e);
            }
        }, userCommonAsyncExecutor);
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<ImportResult> importUsersFromExcelAsync(MultipartFile file) {
        log.info("开始异步导入用户数据从Excel，文件名: {}", file.getOriginalFilename());
        String taskId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            List<String> errors = new ArrayList<>();
            long successCount = 0;
            long failureCount = 0;

            try {
                updateImportTaskStatus(taskId, "RUNNING", 0, 0, 0);

                // TODO: 使用Apache POI解析Excel
                // 这里仅作为示例

                updateImportTaskStatus(taskId, "COMPLETED", successCount + failureCount, successCount, failureCount);

                log.info("用户数据从Excel导入完成，成功: {}, 失败: {}, 耗时: {}ms",
                        successCount, failureCount, System.currentTimeMillis() - startTime);

                return new ImportResult(successCount, failureCount, errors);

            } catch (Exception e) {
                log.error("从Excel导入用户数据失败", e);
                updateImportTaskStatus(taskId, "FAILED", 0, 0, 0);
                errors.add("导入失败: " + e.getMessage());
                return new ImportResult(successCount, failureCount, errors);
            }
        }, userCommonAsyncExecutor);
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<ImportResult> importUsersFromCsvAsync(MultipartFile file) {
        log.info("开始异步导入用户数据从CSV，文件名: {}", file.getOriginalFilename());
        String taskId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            List<String> errors = new ArrayList<>();
            long successCount = 0;
            long failureCount = 0;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

                updateImportTaskStatus(taskId, "RUNNING", 0, 0, 0);

                // 跳过标题行
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IllegalArgumentException("CSV文件为空");
                }

                String line;
                int lineNumber = 1;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    try {
                        // 解析CSV行
                        String[] fields = line.split(",");
                        if (fields.length < 7) {
                            errors.add("第" + lineNumber + "行：字段不足");
                            failureCount++;
                            continue;
                        }

                        // 创建用户
                        UserDTO userDTO = new UserDTO();
                        userDTO.setUsername(fields[1]);
                        userDTO.setNickname(fields[2]);
                        userDTO.setEmail(fields[3]);
                        userDTO.setPhone(fields[4]);
                        userDTO.setUserType(com.cloud.common.enums.UserType.fromCode(fields[5]));
                        userDTO.setStatus(Integer.parseInt(fields[6]));

                        // 保存用户
                        userService.createUser(userDTO);
                        successCount++;

                    } catch (Exception e) {
                        errors.add("第" + lineNumber + "行：" + e.getMessage());
                        failureCount++;
                        log.warn("导入第{}行失败: {}", lineNumber, e.getMessage());
                    }

                    // 每100条更新一次状态
                    if ((successCount + failureCount) % 100 == 0) {
                        updateImportTaskStatus(taskId, "RUNNING",
                                successCount + failureCount, successCount, failureCount);
                    }
                }

                updateImportTaskStatus(taskId, "COMPLETED",
                        successCount + failureCount, successCount, failureCount);

                log.info("用户数据从CSV导入完成，成功: {}, 失败: {}, 耗时: {}ms",
                        successCount, failureCount, System.currentTimeMillis() - startTime);

                return new ImportResult(successCount, failureCount, errors);

            } catch (Exception e) {
                log.error("从CSV导入用户数据失败", e);
                updateImportTaskStatus(taskId, "FAILED", 0, 0, 0);
                errors.add("导入失败: " + e.getMessage());
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
                        status != null ? status.toString() : "UNKNOWN",
                        total != null ? Long.parseLong(total.toString()) : 0,
                        exported != null ? Long.parseLong(exported.toString()) : 0,
                        filePath != null ? filePath.toString() : null
                );

            } catch (Exception e) {
                log.error("获取导出任务状态失败: taskId={}", taskId, e);
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
                        status != null ? status.toString() : "UNKNOWN",
                        total != null ? Long.parseLong(total.toString()) : 0,
                        imported != null ? Long.parseLong(imported.toString()) : 0,
                        failed != null ? Long.parseLong(failed.toString()) : 0
                );

            } catch (Exception e) {
                log.error("获取导入任务状态失败: taskId={}", taskId, e);
                return new ImportTaskStatus(taskId, "ERROR", 0, 0, 0);
            }
        }, userCommonAsyncExecutor);
    }

    /**
     * 更新导出任务状态
     */
    private void updateExportTaskStatus(String taskId, String status, long total, long exported) {
        try {
            String key = "export:task:" + taskId;
            redisTemplate.opsForHash().put(key, "status", status);
            redisTemplate.opsForHash().put(key, "total", total);
            redisTemplate.opsForHash().put(key, "exported", exported);
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("更新导出任务状态失败: taskId={}", taskId);
        }
    }

    /**
     * 更新导入任务状态
     */
    private void updateImportTaskStatus(String taskId, String status, long total, long imported, long failed) {
        try {
            String key = "import:task:" + taskId;
            redisTemplate.opsForHash().put(key, "status", status);
            redisTemplate.opsForHash().put(key, "total", total);
            redisTemplate.opsForHash().put(key, "imported", imported);
            redisTemplate.opsForHash().put(key, "failed", failed);
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("更新导入任务状态失败: taskId={}", taskId);
        }
    }

    /**
     * CSV字段转义
     */
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
