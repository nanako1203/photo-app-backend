package com.caihuan.photo_app_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author nanako
 * @Date 2025/7/31
 * @Description user实体类
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // <-- 添加在这里
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;
    private String password;

    // --- 新增代码开始 ---
    @ManyToMany(fetch = FetchType.EAGER) // 使用 EAGER 模式，方便加载用户时直接获取角色
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
    // --- 新增代码结束 ---
}