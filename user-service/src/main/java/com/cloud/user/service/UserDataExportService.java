package com.cloud.user.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

/**
 * 用户数据导入导出服务
 * 支持大批量用户数据的异步导入导出
 *
 * @author what's up
 */
public interface UserDataExportService {

    /**
     * 异步导出用户数据到Excel
     *
     * @param outputStream 输出流
     * @return 导出结果（导出的记录数）
     */
    CompletableFuture<Long> exportUsersToExcelAsync(OutputStream outputStream);

    /**
     * 异步导出用户数据到CSV
     *
     * @param outputStream 输出流
     * @return 导出结果（导出的记录数）
     */
    CompletableFuture<Long> exportUsersToCsvAsync(OutputStream outputStream);

    /**
     * 异步导入用户数据从Excel
     *
     * @param file Excel文件
     * @return 导入结果（成功数量，失败数量）
     */
    CompletableFuture<ImportResult> importUsersFromExcelAsync(MultipartFile file);

    /**
     * 异步导入用户数据从CSV
     *
     * @param file CSV文件
     * @return 导入结果
     */
    CompletableFuture<ImportResult> importUsersFromCsvAsync(MultipartFile file);

    /**
     * 获取导出任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    CompletableFuture<ExportTaskStatus> getExportTaskStatus(String taskId);

    /**
     * 获取导入任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    CompletableFuture<ImportTaskStatus> getImportTaskStatus(String taskId);

    /**
     * 导入结果类
     */
    class ImportResult {
        private long successCount;
        private long failureCount;
        private java.util.List<String> errors;

        public ImportResult(long successCount, long failureCount, java.util.List<String> errors) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.errors = errors;
        }

        public long getSuccessCount() {
            return successCount;
        }

        public long getFailureCount() {
            return failureCount;
        }

        public java.util.List<String> getErrors() {
            return errors;
        }
    }

    /**
     * 导出任务状态类
     */
    class ExportTaskStatus {
        private String taskId;
        private String status; // PENDING, RUNNING, COMPLETED, FAILED
        private long totalRecords;
        private long exportedRecords;
        private String filePath;

        public ExportTaskStatus(String taskId, String status, long totalRecords, long exportedRecords, String filePath) {
            this.taskId = taskId;
            this.status = status;
            this.totalRecords = totalRecords;
            this.exportedRecords = exportedRecords;
            this.filePath = filePath;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getStatus() {
            return status;
        }

        public long getTotalRecords() {
            return totalRecords;
        }

        public long getExportedRecords() {
            return exportedRecords;
        }

        public String getFilePath() {
            return filePath;
        }
    }

    /**
     * 导入任务状态类
     */
    class ImportTaskStatus {
        private String taskId;
        private String status; // PENDING, RUNNING, COMPLETED, FAILED
        private long totalRecords;
        private long importedRecords;
        private long failedRecords;

        public ImportTaskStatus(String taskId, String status, long totalRecords, long importedRecords, long failedRecords) {
            this.taskId = taskId;
            this.status = status;
            this.totalRecords = totalRecords;
            this.importedRecords = importedRecords;
            this.failedRecords = failedRecords;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getStatus() {
            return status;
        }

        public long getTotalRecords() {
            return totalRecords;
        }

        public long getImportedRecords() {
            return importedRecords;
        }

        public long getFailedRecords() {
            return failedRecords;
        }
    }
}
