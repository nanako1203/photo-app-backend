package com.caihuan.photo_app_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 【推荐修改】使用 @ManyToOne 进行对象关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id", nullable = false)
    private Photo photo;

    // 【推荐修改】将字段重命名为 commenterName 以保持一致
    private String commenterName;

    @Lob
    private String content;

    private Instant createdAt; // 将 createAt 改为 createdAt 是更标准的驼峰命名

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}