package com.caihuan.photo_app_backend.payload.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @Author nanako
 * @Date 2025/8/13
 * @Description 从Google Vision AI返回的多种分析结果
 */

@Data
@Builder //创建对象
public class ImageAnalysisResult {
    // 场景分类结果
    private String sceneCategory;
    // 标签列表
    private List<String> labels;
    // 识别出的文字
    private String detectedText;
    // 主要颜色列表
    private List<String> dominantColors;
}
