package com.caihuan.photo_app_backend.security.jwt;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class LoggingFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        // 获取当前的认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        logger.info("===================== LoggingFilter Start =====================");
        logger.info("Request URI: " + httpServletRequest.getRequestURI());

        if (authentication != null) {
            logger.info("User: " + authentication.getName());
            logger.info("Is Authenticated: " + authentication.isAuthenticated());
            logger.info("Authorities: " + authentication.getAuthorities());
        } else {
            logger.warn("Authentication object is NULL.");
        }
        
        logger.info("===================== LoggingFilter End =======================");

        // 让请求继续往下走
        chain.doFilter(request, response);
    }
}