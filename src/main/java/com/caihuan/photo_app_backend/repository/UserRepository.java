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

   //查找用户
   Optional<User> findByUsername(String username);
   //根据用户名检查用户是否存在
   Boolean existsByUsername(String username);
   //根据邮件检查用户是否存在
   Boolean existsByEmail(String email);
}
