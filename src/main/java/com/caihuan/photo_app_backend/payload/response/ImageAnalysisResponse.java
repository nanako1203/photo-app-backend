package com.caihuan.photo_app_backend.payload.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ImageAnalysisResponse {
    // 【升级】返回一个分类列表
    private List<String> categories;

    // --- 原有字段 ---
    private List<String> labels;
    private String detectedText;
    // private List<String> dominantColors; // 新的Service中已移除，可以注释掉

    // --- 来自 Rekognition 的新字段 ---
    private Integer faceCount;
    private boolean allFacesSmiling;
    private boolean allEyesOpen;
}