package com.caihuan.photo_app_backend.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author nanako
 * @Date 2025/8/1
 * @Description 登录请求参数
 */
@Data
public class LoginRequest {
    //前端发送过来的 username 和 password 字段都不能为空
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
