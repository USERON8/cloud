package com.cloud.user.service;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {






    String uploadAvatar(MultipartFile file);

    String uploadBusinessLicense(Long merchantId, MultipartFile file);

    String getBusinessLicensePresignedUrl(String objectName);
}
