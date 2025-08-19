package com.caihuan.photo_app_backend.payload.request;

import lombok.Data;

import java.util.List;

/**
 * @Author nanako
 * @Date 2025/8/13
 * @Description 请求云端AI精选参数
 */
@Data
public class AiAnalysisRequest {
    private List<Long> photoIds;
}
