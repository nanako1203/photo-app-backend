package com.caihuan.photo_app_backend.controller;

import com.caihuan.photo_app_backend.entity.ERole; // --- 新增 import ---
import com.caihuan.photo_app_backend.entity.Role; // --- 新增 import ---
import com.caihuan.photo_app_backend.entity.User;
import com.caihuan.photo_app_backend.payload.request.LoginRequest;
import com.caihuan.photo_app_backend.payload.request.RegisterRequest;
import com.caihuan.photo_app_backend.payload.response.JwtResponse;
import com.caihuan.photo_app_backend.repository.RoleRepository; // --- 新增 import ---
import com.caihuan.photo_app_backend.repository.UserRepository;
import com.caihuan.photo_app_backend.security.jwt.JwtUtils;
import com.caihuan.photo_app_backend.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet; // --- 新增 import ---
import java.util.Set; // --- 新增 import ---

/**
 * @Author nanako
 * @Date 2025/8/6
 * @Description 登录注册控制器
 */

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    // --- 新增注入 RoleRepository ---
    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body("该用户名已被占用！");
        }

        if(userRepository.existsByEmail(registerRequest.getEmail())){
            return ResponseEntity.badRequest().body("该邮箱已被注册！");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(encoder.encode(registerRequest.getPassword()));

        // --- 新增代码：为用户分配角色 ---
        Set<Role> roles = new HashSet<>();
        // 默认分配 ROLE_USER 角色
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("错误: 数据库中未找到 ROLE_USER 角色。"));
        roles.add(userRole);
        user.setRoles(roles);
        // --- 新增代码结束 ---

        userRepository.save(user);
        return ResponseEntity.ok("用户注册成功");
    }
}