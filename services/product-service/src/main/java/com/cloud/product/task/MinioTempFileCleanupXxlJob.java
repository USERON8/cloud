package com.cloud.product.task;

import com.cloud.common.annotation.DistributedLock;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioTempFileCleanupXxlJob {

  private final MinioClient minioClient;

  @Value("${minio.bucket-name:}")
  private String bucketName;

  @Value("${minio.temp.enabled:true}")
  private boolean enabled;

  @Value("${minio.temp.prefix:temp/}")
  private String prefix;

  @Value("${minio.temp.retention-hours:24}")
  private long retentionHours;

  @Value("${minio.temp.max-delete:500}")
  private int maxDelete;

  @XxlJob("minioTempFileCleanJob")
  @DistributedLock(
      key = "'xxl:product:minio-temp-clean'",
      waitTime = 1,
      leaseTime = 300,
      failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL)
  public void cleanTempFiles() {
    if (!enabled) {
      XxlJobHelper.log("minioTempFileCleanJob skipped, disabled");
      return;
    }
    if (bucketName == null || bucketName.isBlank()) {
      XxlJobHelper.log("minioTempFileCleanJob skipped, bucketName empty");
      return;
    }
    String safePrefix = prefix == null ? "" : prefix.trim();
    if (safePrefix.isEmpty()) {
      XxlJobHelper.log("minioTempFileCleanJob skipped, prefix empty");
      return;
    }
    long safeRetentionHours = retentionHours <= 0 ? 24 : retentionHours;
    int safeMaxDelete = maxDelete <= 0 ? 500 : maxDelete;

    Instant cutoff = Instant.now().minus(Duration.ofHours(safeRetentionHours));
    List<DeleteObject> toDelete = new ArrayList<>();
    int scanned = 0;

    try {
      Iterable<Result<Item>> results =
          minioClient.listObjects(
              ListObjectsArgs.builder()
                  .bucket(bucketName)
                  .prefix(safePrefix)
                  .recursive(true)
                  .build());

      for (Result<Item> result : results) {
        Item item = result.get();
        if (item == null || item.isDir()) {
          continue;
        }
        scanned++;
        Instant lastModified = item.lastModified() == null ? null : item.lastModified().toInstant();
        if (lastModified != null && lastModified.isBefore(cutoff)) {
          toDelete.add(new DeleteObject(item.objectName()));
        }
        if (toDelete.size() >= safeMaxDelete) {
          break;
        }
      }

      if (toDelete.isEmpty()) {
        XxlJobHelper.log("minioTempFileCleanJob finished, scanned=%d, deleted=0", scanned);
        return;
      }

      int failed = 0;
      Iterable<Result<DeleteError>> errors =
          minioClient.removeObjects(
              RemoveObjectsArgs.builder().bucket(bucketName).objects(toDelete).build());
      for (Result<DeleteError> errorResult : errors) {
        DeleteError error = errorResult.get();
        failed++;
        log.warn(
            "Remove temp object failed: object={}, error={}", error.objectName(), error.message());
      }

      int deleted = toDelete.size() - failed;
      XxlJobHelper.log(
          "minioTempFileCleanJob finished, scanned=%d, deleted=%d, failed=%d",
          scanned, deleted, failed);
    } catch (MinioException ex) {
      log.error("minioTempFileCleanJob failed", ex);
      XxlJobHelper.handleFail(ex.getMessage());
    } catch (Exception ex) {
      log.error("minioTempFileCleanJob failed", ex);
      XxlJobHelper.handleFail(ex.getMessage());
    }
  }
}
