package com.caihuan.photo_app_backend.security;

import com.caihuan.photo_app_backend.security.jwt.AuthEntryPointJwt;
import com.caihuan.photo_app_backend.security.jwt.AuthTokenFilter;
import com.caihuan.photo_app_backend.services.UserDetailssServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @Author nanako
 * @Date 2025/8/5
 * @Description 安全规则定义
 */

@Configuration //项目启动时加载并且将里面的设置应用到整个项目
@EnableWebSecurity //更细粒度的方法级别安全控制
public class WebSecurityConfig {
    @Autowired
    UserDetailssServiceImpl userDetailssService;

    //未登录用户访问受保护资源请求
    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    //安检过滤器
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    //认证提供者 去情报官处获取用户信息并使用指定密码编码
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailssService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    //协调登录流程 AuthenticationManager认证管理器
    @Bean
    public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    //比对密码
    private PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        //禁用了CSRF（跨站请求伪造）保护
        http.csrf(csrf -> csrf.disable())
                //认证失败处理
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                //会话管理策略为STATELESS
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                //允许所有对 /api/auth/ 路径（即登录和注册）的访问，不需要通行证
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/auth/**").permitAll()
                //除此之外的任何其他请求，都必须经过认证（必须持有有效的通行证）
                .anyRequest().authenticated());

        //当需要验证用户身份时，请使用我们自己定制的‘认证专家
        http.authenticationProvider(authenticationProvider());
        ////将自定义的AuthTokenFilter（安检过滤器）安插在标准的用户名密码认证过滤器之前
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
