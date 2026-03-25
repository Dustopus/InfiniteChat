package com.dustopus.infinitechat.moment.service.impl;

import com.dustopus.infinitechat.common.exception.BusinessException;
import com.dustopus.infinitechat.common.result.ErrorCode;
import com.dustopus.infinitechat.moment.service.FileUploadService;
import io.minio.GetPresignedUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Override
    public Map<String, String> getUploadUrl(String fileName, String contentType) {
        try {
            // Generate unique object name
            String extension = "";
            if (fileName != null && fileName.contains(".")) {
                extension = fileName.substring(fileName.lastIndexOf("."));
            }
            String objectName = "uploads/" + UUID.randomUUID().toString().replace("-", "") + extension;

            // Generate presigned PUT URL (valid for 1 hour)
            String uploadUrl = minioClient.getPresignedUrl(
                    GetPresignedUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(1, TimeUnit.HOURS)
                            .build());

            // Generate download URL
            String downloadUrl = endpoint + "/" + bucket + "/" + objectName;

            Map<String, String> result = new HashMap<>();
            result.put("uploadUrl", uploadUrl);
            result.put("downloadUrl", downloadUrl);
            result.put("objectName", objectName);
            return result;
        } catch (Exception e) {
            log.error("Failed to generate upload URL", e);
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "生成上传链接失败");
        }
    }

    @Override
    public String getDownloadUrl(String objectName) {
        try {
            return minioClient.getPresignedUrl(
                    GetPresignedUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(24, TimeUnit.HOURS)
                            .build());
        } catch (Exception e) {
            log.error("Failed to generate download URL", e);
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "生成下载链接失败");
        }
    }
}
