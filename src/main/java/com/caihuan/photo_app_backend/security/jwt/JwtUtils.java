package com.caihuan.photo_app_backend.security.jwt;

import com.caihuan.photo_app_backend.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;

import java.security.Key;

/**
 * @Author nanako
 * @Date 2025/8/1
 * @Description 通行证签发与验证
 */
@Component
public class JwtUtils {
    //日志记录器
private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

//注入jwt密钥
@Value("${photoapp.jwt.secret}")
private String jwtSecret;

//注入jwt有效时长
@Value("${photoapp.jwt.expirationMs}")
private int jwtExpirationMs;

//为成功登录的用户生成JWT
public String generateJwtToken(Authentication authentication) {
    //获取对象信息
    UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
    return Jwts.builder()//构建通行证
            .setSubject((userPrincipal.getUsername()))//用户名
            .setIssuedAt(new Date())//签发时间
            .setExpiration(new Date(new Date().getTime() + jwtExpirationMs))//计算并设置过期时间
            .signWith(key(), SignatureAlgorithm.HS256)//给通行证签名
            .compact();//生成最终通行证
}

    //从配置文件中的Base64编码的密钥字符串，生成一个真正可以用于加密的Key对象
private Key key() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
}

//从通行证中读取用户名
public String getUsernameFromJwtToken(String token) {
    return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody().getSubject();
}

//验证token是否生效
public boolean validateJwtToken(String authToken) {
    try {
        Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken);
        return true;
    } catch (
    MalformedJwtException e) {
        logger.error("Invalid JWT token: {}", e.getMessage());
    } catch (
    ExpiredJwtException e) {
        logger.error("JWT token is expired: {}", e.getMessage());
    } catch (
    UnsupportedJwtException e) {
        logger.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
        logger.error("JWT claims string is empty: {}", e.getMessage());
    }
        return false;
}
}
