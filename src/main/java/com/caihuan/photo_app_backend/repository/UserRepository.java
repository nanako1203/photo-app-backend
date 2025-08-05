package com.caihuan.photo_app_backend.repository;

import com.caihuan.photo_app_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @Author nanako
 * @Date 2025/8/1
 * @Description
 * 用户仓库
 */


public interface UserRepository extends JpaRepository<User, Long> {
   Optional<User> findByUsername(String username);
   Boolean existsByUsername(String username);
   Boolean existsByEmail(String email);
}
