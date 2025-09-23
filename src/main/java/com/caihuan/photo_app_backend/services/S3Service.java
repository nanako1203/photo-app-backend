package com.caihuan.photo_app_backend.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import org.springframework.http.MediaType; // 【新增】导入 MediaType

import java.net.URL;
import java.time.Duration;

// ... a

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * @Author nanako
 * @Date 2025/8/5
 * @Description 云存储管理 (重构版)
 */
@Service
@RequiredArgsConstructor //【新增】Lombok 注解
public class S3Service {


    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    // 2. 只注入本服务确实需要的配置项
    @Value("${aws.s3.bucketName}")
    private String bucketName;


    // =================================================================
    // 【新增】生成预签名 URL 的核心方法
    // =================================================================
    public String generatePresignedUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        try {
            // 【新增日志】
            logger.info("S3 Presign: 正在为 Key [{}] 生成预签名URL", objectKey);
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15)) // 设置链接有效期为 15 分钟
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            System.err.println("生成预签名URL失败: " + e.getMessage());
            // 在生产环境中，这里应该使用日志框架，例如 SLF4J
            // log.error("Error generating presigned URL for key {}: {}", objectKey, e.getMessage());
            return null;
        }
    }

    // =================================================================
    // 【新增】从完整 URL 中提取 Object Key 的辅助方法
    // =================================================================
    public String getObjectKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }
        try {
            URL url = new URL(fileUrl);
            // URL.getPath() 会返回如 "/some/path/file.jpg"，我们需要去掉开头的 "/"
            String objectKey = url.getPath().substring(1);
            // 【新增日志】
            logger.info("S3 Parse: 输入的 URL 是 [{}], 解析出的 Key 是 [{}]", fileUrl, objectKey);
            return objectKey;
        } catch (Exception e) {
            System.err.println("从URL提取Object Key失败: " + e.getMessage());
            return null;
        }
    }


    /**
     * 上传文件到S3的核心逻辑。
     * @param requestBody 要上传的文件内容。
     * @param originalFileName 文件的原始名称。
     * @param prefix 要添加到文件名前缀 (例如 "thumb_", "original_")。
     * @return 上传后文件的公开URL。
     */
    private String upload(RequestBody requestBody, String originalFileName, String prefix) {
        String fileName = prefix + UUID.randomUUID().toString() + "_" + originalFileName;
        // 【新增日志】

        logger.info("S3 Upload: 生成的文件名 (Key) 是: [{}]", fileName);

        // 【核心修改】在构建 PutObjectRequest 时，增加 contentType
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(MediaType.IMAGE_JPEG_VALUE) // 告诉 S3 这是一个 JPEG 图片
                .build();


        s3Client.putObject(putObjectRequest, requestBody);

        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(fileName)).toExternalForm();
    }

    /**
     * 从字节数组上传缩略图 (用于本地处理后的结果)。
     */
    public String uploadThumbnail(byte[] fileBytes, String originalFileName) {
        return upload(RequestBody.fromBytes(fileBytes), originalFileName, "thumb_");
    }

    /**
     * 从 MultipartFile 上传全分辨率的原始照片 (用于云端AI分析)。
     */
    public String uploadOriginalPhoto(MultipartFile file) throws IOException {
        return upload(RequestBody.fromInputStream(file.getInputStream(), file.getSize()), file.getOriginalFilename(), "original_");
    }

    /**
     * 从 MultipartFile 上传最终的精修版本。
     */
    public String uploadFinalVersion(MultipartFile file) throws IOException {
        return upload(RequestBody.fromInputStream(file.getInputStream(), file.getSize()), file.getOriginalFilename(), "final_");
    }

    /**
     * 从S3下载文件内容。
     */
    public byte[] downloadFile(String fileUrl) throws IOException {
        try {
            URI uri = new URI(fileUrl);
            String key = uri.getPath().substring(1);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> objectAsBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return objectAsBytes.asByteArray();
        } catch (Exception e) {
            throw new IOException("无法从S3下载文件: " + fileUrl, e);
        }
    }

// =================================================================
// 【新增】一个专门通过 Object Key 下载文件的方法
// =================================================================
    /**
     * 根据 S3 的 Object Key 下载文件内容
     * @param objectKey S3 中的文件名/路径
     * @return 文件的字节数组
     * @throws IOException 下载失败时抛出异常
     */
    public byte[] downloadFileByKey(String objectKey) throws IOException {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return objectBytes.asByteArray();
        } catch (S3Exception e) {
            // 捕获更具体的 S3 异常，便于排查
            throw new IOException("无法从S3下载文件 (key: " + objectKey + "): " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * 从S3删除一个文件。
     */
    public void deleteFile(String fileUrl) throws URISyntaxException {
        URI uri = new URI(fileUrl);
        String key = uri.getPath().substring(1);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName).key(key).build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    // ====================== 【新增的核心方法】 ======================
    /**
     * 从字节数组上传用于AI分析的图片 (例如1280px的预览图)，并返回其 S3 Object Key。
     * @param fileBytes 文件的字节数组
     * @param originalFileName 原始文件名
     * @return S3 Object Key
     */
    public String uploadAnalysisImageAndReturnKey(byte[] fileBytes, String originalFileName) {
        // 1. 生成唯一的Key (文件名)
        String key = "analysis_" + UUID.randomUUID().toString() + "_" + originalFileName;
        logger.info("S3 Upload (for Analysis): 生成的文件 Key 是: [{}]", key);

        // 2. 构建请求
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(MediaType.IMAGE_JPEG_VALUE)
                .build();

        // 3. 上传文件
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

        // 4. 直接返回 Key
        return key;
    }
}

