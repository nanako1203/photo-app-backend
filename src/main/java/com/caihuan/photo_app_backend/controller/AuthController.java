package com.caihuan.photo_app_backend.controller;

import com.caihuan.photo_app_backend.entity.User;
import com.caihuan.photo_app_backend.payload.request.LoginRequest;
import com.caihuan.photo_app_backend.payload.request.RegisterRequest;
import com.caihuan.photo_app_backend.payload.response.JwtResponse;
import com.caihuan.photo_app_backend.repository.UserRepository;
import com.caihuan.photo_app_backend.security.jwt.JwtUtils;
import com.caihuan.photo_app_backend.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * @Author nanako
 * @Date 2025/8/6
 * @Description 登录注册控制器
 */

@RestController//所有方法返回json
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)//预检请求缓存3600秒
public class AuthController {

    //处理登录验证
    @Autowired
    AuthenticationManager authenticationManager;

    //查询数据库的user表
    @Autowired
    UserRepository userRepository;

    //密码编码器
    @Autowired
    PasswordEncoder encoder;

    //生成和验证jwt
    @Autowired
    JwtUtils jwtUtils;

    //@RequestBody 将http中json转成loginRequest对象
    // @Valid 触发对字段的验证
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest) {
        //接受前端封装成 UsernamePasswordAuthenticationTokenspringsecurity比对账号密码
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        //将用户信息放入SecurityContextHolder
        SecurityContextHolder.getContext().setAuthentication(authentication);
        //生成jwt通行证
        String jwt = jwtUtils.generateJwtToken(authentication);
        //获取用户详细信息
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        //成功的json响应 返回给前
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
            return ResponseEntity.badRequest().body("该邮箱以被注册！");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(encoder.encode(registerRequest.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("用户注册成功");
    }
}
