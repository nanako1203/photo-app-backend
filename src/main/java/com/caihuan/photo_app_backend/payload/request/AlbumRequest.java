package com.caihuan.photo_app_backend.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author nanako
 * @Date 2025/8/13
 * @Description 相册请求参数
 */
@Data
public class AlbumRequest {

    //相册名称
    @NotBlank
    private String name;
}
