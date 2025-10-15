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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageAnalysisService {

    private final RekognitionClient rekognitionClient;
    private static final Logger logger = LoggerFactory.getLogger(ImageAnalysisService.class);

    @Value("${AWS_S3_BUCKET_NAME}")
    private String bucketName;

    @PostConstruct
    public void init() {
        logger.info("初始化 ImageAnalysisService... 读取到的 S3 桶名是: '{}'", bucketName);
    }

    public ImageAnalysisResponse analyzeImageFromS3(String objectKey) throws IOException {
        try {
            S3Object s3Object = S3Object.builder().bucket(bucketName).name(objectKey).build();
            Image imageToAnalyze = Image.builder().s3Object(s3Object).build();

            // API请求部分保持不变
            DetectLabelsRequest labelsRequest = DetectLabelsRequest.builder()
                    .image(imageToAnalyze)
                    .maxLabels(20) // 增加标签数量以提高分类准确性
                    .minConfidence(70F).build();

            DetectFacesRequest facesRequest = DetectFacesRequest.builder()
                    .image(imageToAnalyze)
                    .attributes(Attribute.ALL).build();

            DetectTextRequest textRequest = DetectTextRequest.builder().image(imageToAnalyze).build();

            DetectLabelsResponse labelsResponse = rekognitionClient.detectLabels(labelsRequest);
            DetectFacesResponse facesResponse = rekognitionClient.detectFaces(facesRequest);
            DetectTextResponse textResponse = rekognitionClient.detectText(textRequest);

            // --- 数据提取与全新分类逻辑 ---

            List<Label> rawLabels = labelsResponse.labels();
            List<String> labelNames = rawLabels.stream().map(Label::name).collect(Collectors.toList());

            // 【核心升级】调用全新的多维度分类方法
            List<String> categories = classifyPhoto(rawLabels, textResponse.textDetections());

            String detectedText = textResponse.textDetections().stream()
                    .filter(td -> td.type() == TextTypes.LINE && td.confidence() > 75F)
                    .map(TextDetection::detectedText)
                    .collect(Collectors.joining("\n"));

            List<FaceDetail> faceDetails = facesResponse.faceDetails();
            Integer faceCount = faceDetails.size();
            boolean allFacesSmiling = !faceDetails.isEmpty() && faceDetails.stream()
                    .allMatch(face -> face.smile() != null && face.smile().value());
            boolean allEyesOpen = !faceDetails.isEmpty() && faceDetails.stream()
                    .allMatch(face -> face.eyesOpen() != null && face.eyesOpen().value());

            // 返回给PhotoService的数据结构，增加了categories列表
            return ImageAnalysisResponse.builder()
                    .labels(labelNames)       // 返回原始标签列表
                    .categories(categories)   // 【新增】返回计算出的分类列表
                    .detectedText(detectedText)
                    .faceCount(faceCount)
                    .allFacesSmiling(allFacesSmiling)
                    .allEyesOpen(allEyesOpen)
                    .build();

        } catch (RekognitionException e) {
            logger.error("AWS Rekognition S3 分析模式失败 (Key: {}): {}", objectKey, e.awsErrorDetails().errorMessage());
            throw new IOException("AWS Rekognition S3 分析模式失败", e);
        }
    }

    /**
     * 【全新多维度分类方法】
     * 根据标签和文字，为照片打上多个分类标签
     * @param labels AWS返回的原始Label对象列表
     * @param textDetections AWS返回的文字检测结果
     * @return 一个包含所有匹配分类的字符串列表
     */
    private List<String> classifyPhoto(List<Label> labels, List<TextDetection> textDetections) {
        Set<String> labelSet = labels.stream()
                .map(label -> label.name().toLowerCase())
                .collect(Collectors.toSet());

        List<String> categories = new ArrayList<>();

        // --- 维度一: 内容主题 ---
        if (labelSet.contains("person") || labelSet.contains("face") || labelSet.contains("portrait")) categories.add("人像");
        if (labelSet.contains("landscape") || labelSet.contains("nature") || labelSet.contains("sky") || labelSet.contains("mountain") || labelSet.contains("sea")) categories.add("风景");
        if (labelSet.contains("architecture") || labelSet.contains("building") || labelSet.contains("cityscape")) categories.add("建筑");
        if (labelSet.contains("animal") || labelSet.contains("pet") || labelSet.contains("dog") || labelSet.contains("cat") || labelSet.contains("bird")) categories.add("动物");
        if (labelSet.contains("food") || labelSet.contains("dining") || labelSet.contains("dessert") || labelSet.contains("drink") || labelSet.contains("restaurant")) categories.add("美食");
        if (labelSet.contains("car") || labelSet.contains("vehicle") || labelSet.contains("train") || labelSet.contains("airplane") || labelSet.contains("boat") || labelSet.contains("bicycle")) categories.add("交通工具");
        if (labelSet.contains("flower") || labelSet.contains("plant") && !labelSet.contains("potted plant")) categories.add("自然细节");
        if (labelSet.contains("sports") || labelSet.contains("stadium") || labelSet.contains("soccer") || labelSet.contains("basketball")) categories.add("体育运动");

        // --- 维度二: 事件与场合 ---
        if (labelSet.contains("party") || labelSet.contains("celebration") || labelSet.contains("birthday cake") || labelSet.contains("balloons") || labelSet.contains("confetti")) categories.add("派对与庆祝");
        if (labelSet.contains("concert") || labelSet.contains("stage") || labelSet.contains("performance") || labelSet.contains("crowd") || labelSet.contains("musical instrument")) categories.add("演出活动");
        if (labelSet.contains("fireworks")) categories.add("节日烟火");
        if (labelSet.contains("christmas tree")) categories.add("节日");


        // --- 维度三: 照片属性与用途 ---
        // 通过检测到的文字数量和标签来判断
        boolean hasSignificantText = textDetections.stream().anyMatch(td -> td.type() == TextTypes.LINE);
        if (hasSignificantText && (labelSet.contains("text") || labelSet.contains("document") || labelSet.contains("paper") || labelSet.contains("screenshot") || labelSet.contains("receipt"))) {
            categories.add("文档与截图");
        }
        if (labelSet.contains("monochrome") || labelSet.contains("black and white")) categories.add("黑白照片");
        if (labelSet.contains("panorama")) categories.add("全景照片");

        // 如果没有任何特定分类，则归为“其他”
        if (categories.isEmpty()) {
            categories.add("其他");
        }

        return categories;
    }
}