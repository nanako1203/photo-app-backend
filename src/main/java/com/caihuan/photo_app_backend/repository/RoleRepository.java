package com.caihuan.photo_app_backend.repository;

import com.caihuan.photo_app_backend.entity.ERole;
import com.caihuan.photo_app_backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    // 根据角色名称查找角色
    Optional<Role> findByName(ERole name);
}