package com.caihuan.photo_app_backend.services;

import com.caihuan.photo_app_backend.payload.dto.ImageAnalysisResult;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @Author nanako
 * @Date 2025/8/14
 * @Description 负责所有与Google Vision AI的交互
 */

@Service
public class ImageAnalysisService {

    //创建日志记录器
    private static final Logger logger = (Logger) LoggerFactory.getLogger(ImageAnalysisService.class);

    // 从MultipartFile分析 (用于桌面客户端初次上传)
    public ImageAnalysisResult analyzeImage(MultipartFile file) throws IOException {
        return performAnalysis(file.getBytes());
    }

    // 从字节数组分析 (用于Web端AI精选)
    public ImageAnalysisResult analyzeImageBytes(byte[] imageBytes) {
        return performAnalysis(imageBytes);
    }

    //核心ai分析方法
    private ImageAnalysisResult performAnalysis(byte[] imageBytes) {

        //建立通信
        try(ImageAnnotatorClient vison = ImageAnnotatorClient.create()) {

            //上传文件字节数组转为Google Vision API需要的格式
            ByteString imgBytes = ByteString.copyFrom(imageBytes);
            Image img = Image.newBuilder().setContent(imgBytes).build();

            // 2. 构建一个包含多种特征请求的列表
            ArrayList<Feature> features = new ArrayList<>();

            //请求最多10个标签
            features.add(Feature.newBuilder()
                    .setType(Feature.Type.LABEL_DETECTION)
                    .setMaxResults(10)
                    .build());

            //请求进行文字识别
            features.add(Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build());

            //分析图像属性
            features.add(Feature.newBuilder()
                    .setType(Feature.Type.IMAGE_PROPERTIES)
                    .build());

            //自动化安全阈 识别不合适的图片
            features.add(Feature.newBuilder()
                    .setType(Feature.Type.SAFE_SEARCH_DETECTION)
                    .build());

            // 将所有请求特征和图片打包成一个AnnotateImageRequest
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addAllFeatures(features)
                    .setImage(img)
                    .build();

            // 将请求发送给Google Vision API，并获取响应
            BatchAnnotateImagesResponse response = vison.batchAnnotateImages(List.of(request));
            
            //获取图片
            AnnotateImageResponse res = response.getResponses(0);
            
            //检查错误
            if (res.hasError()){
                logger.error("Google Vision API Error: {}", res.getError().getMessage());

                //返回一个空的分析结果
                return ImageAnalysisResult.builder().build();
            }

            // 5. 解析标签和场景分类
            List<String> labels = res.getLabelAnnotationsList()
                    .stream()
                    //提取标签的描述文字
                    .map(EntityAnnotation::getDescription)
                    //搜集成列表
                    .collect(Collectors.toList());

            // 调用辅助方法进行场景分类
            String sceneCategory = classifySceneFromLabels(labels);

            //解析文字识别结果
            String detectedText = res.getFullTextAnnotation().getText();

            //解析主要颜色
            List<String> dominantColors = res.getImagePropertiesAnnotation().
                    getDominantColors()
                    .getColorsList()
                    .stream()
                    .map(colorInfo -> String.format("#%02x%02x%02x", // 将RGB颜色值格式化为十六进制字符串
                            (int) colorInfo.getColor().getRed(),
                            (int) colorInfo.getColor().getGreen(),
                            (int) colorInfo.getColor().getBlue()))
                    .limit(5)//只取前五个颜色
                    .collect(Collectors.toList());

            return ImageAnalysisResult.builder()
                    .sceneCategory(sceneCategory)
                    .labels(labels)
                    .detectedText(detectedText)
                    .dominantColors(dominantColors)
                    .build();


        } catch (IOException e) {
            logger.error("Google Vision API Error: {}", e);
        }
        return ImageAnalysisResult.builder().build();
    }

    //根据标签列表进行场景分类
    private String classifySceneFromLabels(List<String> labels) {
        Set<String> labelSet = labels.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        if (labelSet.contains("close-up") || labelSet.contains("macro")){
            return "特写";
        }
        if (labelSet.contains("portrait") || labelSet.contains("person") || labelSet.contains("face")){
            return "中景";
        }
        if (labelSet.contains("landscape") || labelSet.contains("sky") || labelSet.contains("mountain")
                || labelSet.contains("sea")){
            return "远景";
        }
        return "未分类";
    }
}
