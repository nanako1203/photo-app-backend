package com.caihuan.photo_app_backend.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author nanako
 * @Date 2025/9/14
 * @Description 用于接收前端云端AI分析请求的DTO
 */
@Data
public class CloudAnalysisRequest {
    @NotBlank(message = "imageBase64 不能为空")
    private String imageBase64;
}

