package com.caihuan.photo_app_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @Author nanako
 * @Date 2025/8/13
 * @Description 评论类
 */

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
public class Comment {

    //评论id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 这条评论所属照片的ID
    private Long photoId;

    // 评论者姓名
    private String contentName;

    //评论
    @Lob
    private String content;

    private Instant createAt;

    @PrePersist
    public void prePersist() {
        if (createAt == null) {
            createAt = Instant.now();
        }
    }
}
