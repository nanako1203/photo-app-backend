package com.caihuan.photo_app_backend.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author nanako
 * @Date 2025/8/13
 * @Description评论请求参数
 */
@Data
public class CommentRequest {
    //评论者姓名
    @NotBlank
    private String commenterName;

    //评论内容
    @NotBlank
    private String content;

}
