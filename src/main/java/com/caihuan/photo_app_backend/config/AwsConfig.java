package com.caihuan.photo_app_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsConfig {

    @Value("${aws.accessKeyId}")
    private String accessKey;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.s3.region}")
    private String region;

    @Bean
    public StaticCredentialsProvider staticCredentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    @Bean
    public Region awsRegion() {
        return Region.of(region);
    }

    @Bean
    public S3Client s3Client(Region region, StaticCredentialsProvider credentialsProvider) {
        return S3Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }
    
    @Bean
    public S3Presigner s3Presigner(Region region, StaticCredentialsProvider credentialsProvider) {
        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }
    
    @Bean
    public RekognitionClient rekognitionClient(Region region, StaticCredentialsProvider credentialsProvider) {
        return RekognitionClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }
}