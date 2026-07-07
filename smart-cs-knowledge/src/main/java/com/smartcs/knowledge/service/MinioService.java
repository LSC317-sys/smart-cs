package com.smartcs.knowledge.service;

import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MinIO 文件存储服务
 */
@Slf4j
@Service
public class MinioService {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioService(MinioClient minioClient,
                        @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        initBucket();
    }

    private void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket 创建成功: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO bucket 初始化失败 (可能服务未启动): {}", e.getMessage());
        }
    }

    /**
     * 上传文件到 MinIO，返回存储路径
     */
    public String uploadFile(MultipartFile file, String docId) throws IOException, Exception {
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = docId + ext;

        // 先写临时文件
        Path tempFile = Files.createTempFile("upload_", ext);
        file.transferTo(tempFile.toFile());

        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(Files.newInputStream(tempFile), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            log.info("文件已上传到 MinIO: bucket={}, object={}, size={}", bucket, objectName, file.getSize());
            return objectName;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * 从 MinIO 下载文件流
     */
    public InputStream getFile(String objectName) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("从 MinIO 获取文件失败: " + objectName, e);
        }
    }

    /**
     * 删除 MinIO 中的文件
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build()
            );
            log.info("MinIO 文件已删除: {}", objectName);
        } catch (Exception e) {
            log.warn("MinIO 文件删除失败: {} ({})", objectName, e.getMessage());
        }
    }

    public String getBucket() {
        return bucket;
    }
}
