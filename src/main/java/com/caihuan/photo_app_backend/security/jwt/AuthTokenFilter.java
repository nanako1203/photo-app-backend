package com.caihuan.photo_app_backend.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @Author nanako
 * @Date 2025/8/1
 * @Description 检查是否携带jwt
 */

//extends OncePerRequestFilter只检查一次
public class AuthTokenFilter extends OncePerRequestFilter {

    //注入通行证签发与验证中心
    @Autowired
    private JwtUtils jwtUtils;

    //注入情报官 调取用户档案
    @Autowired
    private UserDetailsService userDetailsService;

    //核心工作方法
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);
            //验证jwt
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                //调取用户名
                String username = jwtUtils.getUsernameFromJwtToken(jwt);
                //解析用户名
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                //放进用户的完整档案 凭证 权限列表
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                //添加ip地址等细节
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                //将认证记录给spring
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }
        //检查完放行
        filterChain.doFilter(request, response);
    }

    //查找Authorization
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth)  && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
