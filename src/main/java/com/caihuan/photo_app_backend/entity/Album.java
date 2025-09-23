package com.caihuan.photo_app_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @Author nanako
 * @Date 2025/7/31
 * @Description 相册实体类
 */
@Entity
@Table(name = "albums")
@Data
@NoArgsConstructor
// 【重要】请确保添加了这个注解
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Long userId;
    private String shareToken;

    @PrePersist//// JPA生命周期注解：在对象第一次被保存到数据库之前，会自动执行下面的方法
    private void prePersist() {
        if (shareToken == null) {
            shareToken = UUID.randomUUID().toString();
        }
    }
}
