package com.caihuan.photo_app_backend.services;

import jakarta.annotation.PostConstruct;
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

/**
 * @Author nanako
 * @Date 2025/8/5
 * @Description 云存储管理
 */

@Service
public class S3Service {

    @Value("${aws.accessKeyId}")
    private String accessKey;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    private S3Client s3Client;

    //初始化链接
    @PostConstruct// 在这个服务被创建后，立刻执行此方法
    private void initializeS3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    //web端上传文件
    public String upLoadFile(MultipartFile file) throws IOException {
        //创建不会重复的文件名
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        //上传指令
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        //实际上传动作
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        //返回上传后的url
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(fileName)).toExternalForm();

    }

    //从桌面端上传
    public String uploadFile(byte[] fileBytes, String originalFileName) {
        String fileName = "thumb_" + UUID.randomUUID().toString() + "_" + originalFileName;
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(fileName)).toExternalForm();

    }

    //上传最终精修版本
    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = "final_" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(fileName)).toExternalForm();

    }

    //从s3下载文件内容
    public byte[] downloadFile(String fileUrl) throws IOException {
        try {
            URI uri = new URI(fileUrl);
            //获取key
            String key = uri.getPath().substring(1);
            //创建下载指令
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            //执行下载操作,文件内容作为字节数组返回
            ResponseBytes<GetObjectResponse> objectAsBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return objectAsBytes.asByteArray();
        }catch (Exception e) {
            throw new IOException("无法下载文件：" + fileUrl, e);
        }
    }

    //从s3删除一个文件
    public void deleteFile(String fileUrl) throws URISyntaxException {
        URI uri = new URI(fileUrl);
        String key = uri.getPath().substring(1);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName).key(key).build();
        s3Client.deleteObject(deleteObjectRequest);
    }

}
