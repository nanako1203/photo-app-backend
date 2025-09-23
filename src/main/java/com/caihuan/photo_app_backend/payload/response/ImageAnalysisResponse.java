package com.caihuan.photo_app_backend.payload.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * @Author nanako
 * @Date 2025/9/13
 * @Description 用于封装AI分析结果并返回给前端的DTO（已合并AWS Rekognition功能）
 */
@Data
@Builder
public class ImageAnalysisResponse {
    // --- 原有字段 ---
    private String sceneCategory;
    private List<String> labels;
    private String detectedText;
    private List<String> dominantColors;

    // --- 【新增】来自 Rekognition 的新字段 ---
    //private Double sharpness;
    //private Double brightness;
    private Integer faceCount;
    private boolean allFacesSmiling;
    private boolean allEyesOpen;
}