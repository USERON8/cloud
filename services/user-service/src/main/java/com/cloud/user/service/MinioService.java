package com.cloud.user.service;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {

  String uploadAvatar(MultipartFile file);

  String uploadBusinessLicense(Long merchantId, MultipartFile file);

  String uploadIdCardFront(Long merchantId, MultipartFile file);

  String uploadIdCardBack(Long merchantId, MultipartFile file);

  String getCertPresignedUrl(String objectName);
}
