package com.caihuan.photo_app_backend.payload.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * @Author nanako
 * @Date 2025/8/13
 * @Description 桌面客户端和后端之间传输的数字档案
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhotoArchiveDto {
    // 原始文件名
    private String originalFileName;
    // 缩略图的Base64编码字符串
    private String thumbnailBase64;
    // 原始文件的哈希值
    private String fileHash;
    // 照片的元数据
    private Map<String, String> metadata;
    // 是否模糊 (本地AI分析结果)
    private boolean isBlurry;
    // 是否过曝 (本地AI分析结果)
    private boolean isOverExposed;
    // 照片所在的本地文件夹名
    private String localCategory;
    // 添加下面这个字段
    private String pHash;

    // 【请在这里新增下面这个字段】
    private String previewBase64;
    // =======================================================
}
