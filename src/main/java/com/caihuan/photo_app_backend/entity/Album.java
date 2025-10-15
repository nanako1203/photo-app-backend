package com.caihuan.photo_app_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "albums")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String shareToken;

    // =======================================================
    // ==           【新增】与 User 实体的关联              ==
    // =======================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 这会创建一个 user_id 外键列
    @JsonIgnore // 在序列化Album时，避免无限循环
    @ToString.Exclude // 避免Lombok的toString方法导致无限循环
    private User user;

    // =======================================================
    // ==           【新增】与 Photo 实体的关联             ==
    // =======================================================
    @OneToMany(
            mappedBy = "album",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore // 通常在获取相册列表时，我们不需要同时加载所有照片
    @ToString.Exclude
    private List<Photo> photos = new ArrayList<>();


    @PrePersist
    private void prePersist() {
        if (shareToken == null) {
            shareToken = UUID.randomUUID().toString();
        }
    }
}