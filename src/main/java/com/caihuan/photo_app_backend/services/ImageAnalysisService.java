package com.caihuan.photo_app_backend.services;

import com.caihuan.photo_app_backend.payload.response.ImageAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageAnalysisService {

    private final RekognitionClient rekognitionClient;
    private static final Logger logger = LoggerFactory.getLogger(ImageAnalysisService.class);

    // 从 application.properties 注入您的S3存储桶名称
    @Value("${AWS_S3_BUCKET_NAME}")
    private String bucketName;
    @PostConstruct
    public void init() {
        logger.info("初始化 ImageAnalysisService... 读取到的 S3 桶名是: '{}'", bucketName);
        if (bucketName == null || bucketName.isEmpty()) {
            logger.error("严重错误: S3 桶名未能从配置中加载！请检查 application.properties 中的 AWS_S3_BUCKET_NAME 配置！");
        }
    }

    /**
     * 【全新方法】让 Rekognition 直接从 S3 分析图片，取代旧的 analyzeImageBytes 方法
     * @param objectKey S3 中对象的 Key (也即文件名)
     * @return 包含所有分析结果的 ImageAnalysisResponse 对象
     */

    // 请使用这个最终修正版的完整方法
    public ImageAnalysisResponse analyzeImageFromS3(String objectKey) throws IOException {
        try {
            // 1. 构建 Image 对象 (不变)
            S3Object s3Object = S3Object.builder()
                    .bucket(bucketName)
                    .name(objectKey)
                    .build();
            Image imageToAnalyze = Image.builder().s3Object(s3Object).build();

            // 2. 使用我们验证过的【简化版】请求来检测标签
            DetectLabelsRequest labelsRequest = DetectLabelsRequest.builder()
                    .image(imageToAnalyze)
                    .maxLabels(10)
                    .minConfidence(75F)
                    .build();

            // 3. 构建人脸检测请求 (不变)
            DetectFacesRequest facesRequest = DetectFacesRequest.builder()
                    .image(imageToAnalyze)
                    .attributes(Attribute.ALL)
                    .build();

            // 4. 构建文字检测请求 (不变)
            DetectTextRequest textRequest = DetectTextRequest.builder().image(imageToAnalyze).build();

            // 5. 重新启用所有的AWS API调用
            logger.info("向 AWS 发送 DetectLabelsRequest (S3 模式): {}", objectKey);
            DetectLabelsResponse labelsResponse = rekognitionClient.detectLabels(labelsRequest);

            logger.info("向 AWS 发送 DetectFacesRequest (S3 模式): {}", objectKey);
            DetectFacesResponse facesResponse = rekognitionClient.detectFaces(facesRequest);

            logger.info("向 AWS 发送 DetectTextRequest (S3 模式): {}", objectKey);
            DetectTextResponse textResponse = rekognitionClient.detectText(textRequest);

            // 6. 从各个响应中提取所需数据
            List<String> labels = labelsResponse.labels().stream()
                    .map(Label::name)
                    .collect(Collectors.toList());

            // 【重大修正】移除对 sharpness 和 brightness 的获取
            // Double sharpness = null;
            // Double brightness = null;

            // 【重大修正】修正 textDetections() 的拼写错误
            String detectedText = textResponse.textDetections().stream()
                    .filter(td -> td.type() == TextTypes.LINE && td.confidence() > 75F)
                    .map(TextDetection::detectedText)
                    .collect(Collectors.joining("\n"));

            List<FaceDetail> faceDetails = facesResponse.faceDetails();
            Integer faceCount = faceDetails.size();
            boolean allFacesSmiling = !faceDetails.isEmpty() && faceDetails.stream()
                    .allMatch(face -> {
                        Smile smile = face.smile();
                        return smile != null && smile.value() && smile.confidence() > 80F;
                    });
            boolean allEyesOpen = !faceDetails.isEmpty() && faceDetails.stream()
                    .allMatch(face -> face.eyesOpen() != null && face.eyesOpen().value());

            String sceneCategory = classifySceneFromLabels(labels);

            // 7. 构建并返回最终的响应对象 (已移除 sharpness 和 brightness)
            return ImageAnalysisResponse.builder()
                    .labels(labels)
                    .detectedText(detectedText)
                    // .sharpness(sharpness) // 移除
                    // .brightness(brightness) // 移除
                    .faceCount(faceCount)
                    .allFacesSmiling(allFacesSmiling)
                    .allEyesOpen(allEyesOpen)
                    .sceneCategory(sceneCategory)
                    .dominantColors(Collections.emptyList()) // 主色调也暂时移除
                    .build();

        } catch (RekognitionException e) {
            logger.error("AWS Rekognition S3 分析模式失败 (Key: {}): {}", objectKey, e.awsErrorDetails().errorMessage());
            throw new IOException("AWS Rekognition S3 分析模式失败", e);
        }
    }

    private String classifySceneFromLabels(List<String> labels) {
        Set<String> labelSet = labels.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        if (labelSet.contains("close-up") || labelSet.contains("macro photography")){
            return "特写";
        }
        if (labelSet.contains("portrait") || labelSet.contains("person") || labelSet.contains("face")){
            return "人像";
        }
        if (labelSet.contains("landscape") || labelSet.contains("sky") || labelSet.contains("mountain")
                || labelSet.contains("sea") || labelSet.contains("nature")){
            return "风景";
        }
        return "静物/其他";
    }
}

