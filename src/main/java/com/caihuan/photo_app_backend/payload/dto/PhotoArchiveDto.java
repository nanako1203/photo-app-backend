package com.caihuan.photo_app_backend.payload.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// 这个注解很有用，可以防止因为前端多传了未知字段而后端报错
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PhotoArchiveDto {

    // 照片元数据
    private String originalFileName;
    private String localCategory;
    private String fileHash;
    private String pHash;

    // 质量评分
    private double qualityScore;
    private boolean isBlurry;
    private boolean isOverExposed;

    // 图片数据 (在“最终稿”模式下可以为 null)
    private String thumbnailBase64;
    private String previewBase64;
}