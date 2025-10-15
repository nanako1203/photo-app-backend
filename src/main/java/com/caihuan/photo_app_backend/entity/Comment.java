package com.caihuan.photo_app_backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference; // 【新增】导入这个注解
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id", nullable = false)
    @JsonBackReference // 【新增】添加这个注解来防止序列化循环
    private Photo photo;

    private String commenterName;

    @Lob
    private String content;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}