package com.caihuan.photo_app_backend.services;

import com.caihuan.photo_app_backend.entity.ERole;
import com.caihuan.photo_app_backend.entity.Role;
import com.caihuan.photo_app_backend.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时，自动初始化角色数据
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // 检查 ROLE_USER 是否存在，如果不存在则创建
        if (roleRepository.findByName(ERole.ROLE_USER).isEmpty()) {
            roleRepository.save(new Role(ERole.ROLE_USER));
        }

        // 检查 ROLE_ADMIN 是否存在，如果不存在则创建
        if (roleRepository.findByName(ERole.ROLE_ADMIN).isEmpty()) {
            roleRepository.save(new Role(ERole.ROLE_ADMIN));
        }

        // 检查 ROLE_MODERATOR 是否存在，如果不存在则创建
        if (roleRepository.findByName(ERole.ROLE_MODERATOR).isEmpty()) {
            roleRepository.save(new Role(ERole.ROLE_MODERATOR));
        }
    }
}