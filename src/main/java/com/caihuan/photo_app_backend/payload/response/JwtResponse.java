package com.caihuan.photo_app_backend.payload.response;

import lombok.Data;

/**
 * @Author nanako
 * @Date 2025/8/1
 * @Description 登录响应参数
 */
@Data
public class JwtResponse {
    private String token;
    //国际通行的行业标准。
    //这个标准叫做 RFC 6750，它规定了如何安全地在网络请求中携带“令牌 (Token)”。
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;

    public JwtResponse(String accessToken, Long id, String username, String email) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
    }
}
