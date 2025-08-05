package com.caihuan.photo_app_backend.security.services;

import com.caihuan.photo_app_backend.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @Author nanako
 * @Date 2025/8/3
 * @Description 将User 实体类，包装成Spring Security框架的标准 UserDetails
 */
public class UserDetailsImpl implements UserDetails {

    //
    private static final long serialVersionUID = 1L;//序列化版本号

    private Long id;
    private String username;
    private String email;

    @JsonIgnore//反馈给前端的数据不打包密码
    private String password;

    public UserDetailsImpl(Long id, String username, String email, String password) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    //将一个从数据库查出来的 User 对象，转换成一个 UserDetailsImpl 对象
    public static UserDetailsImpl build(User user) {
        return new UserDetailsImpl(user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword());
    }

    //返回用户的权限列表（比如“管理员”、“普通用户”等）。
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    //账号是否过期
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    //账号是否锁定
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    //凭证是否过期
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    //账号是否可用
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass() ) {return false;}
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }

}
