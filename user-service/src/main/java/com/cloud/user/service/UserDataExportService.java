package com.cloud.user.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;







public interface UserDataExportService {

    





    CompletableFuture<Long> exportUsersToExcelAsync(OutputStream outputStream);

    





    CompletableFuture<Long> exportUsersToCsvAsync(OutputStream outputStream);

    





    CompletableFuture<ImportResult> importUsersFromExcelAsync(MultipartFile file);

    





    CompletableFuture<ImportResult> importUsersFromCsvAsync(MultipartFile file);

    





    CompletableFuture<ExportTaskStatus> getExportTaskStatus(String taskId);

    





    CompletableFuture<ImportTaskStatus> getImportTaskStatus(String taskId);

    


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

    


    class ExportTaskStatus {
        private String taskId;
        private String status; 
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

    


    class ImportTaskStatus {
        private String taskId;
        private String status; 
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
