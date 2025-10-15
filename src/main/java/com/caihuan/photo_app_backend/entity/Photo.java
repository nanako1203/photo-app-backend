package com.caihuan.photo_app_backend.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "photos")
@Data
@NoArgsConstructor
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    private String storageUrl;
    private boolean isLikedByClient = false;
    private boolean isFinalized = false;
    private String finalStorageUrl;
    private String originalFileName;

    // 【升级】存储多个场景分类，例如：["人像", "派对"]
    // JPA会自动创建一个名为 "photo_categories" 的表来存储这些值
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> categories;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> aiLabels;

    @Lob
    private String aiDetectedText;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> aiDominantColors;

    private String localCategory;
    private boolean cloudAnalyzed = false;

    // 质量评估字段 (在新的ImageAnalysisResponse中已移除，可以保留)
    private Double sharpness;
    private Double brightness;

    // 人脸分析字段
    private Integer faceCount;
    private boolean allFacesSmiling;
    private boolean allEyesOpen;

    @Column(name = "analysis_image_key")
    private String analysisImageKey;

    // =======================================================
    // ==           【新增】一个临时的、非数据库字段          ==
    // =======================================================
    @Transient // 这个注解告诉JPA不要为这个字段在数据库中创建列
    private String previewUrl;

    // =======================================================
    // ==           【核心修改】添加级联删除设置            ==
    // =======================================================
    @OneToMany(
            mappedBy = "photo",
            cascade = CascadeType.ALL, // <-- 新增
            orphanRemoval = true,      // <-- 新增
            fetch = FetchType.LAZY
    )
    @JsonManagedReference
    @ToString.Exclude
    private List<Comment> comments = new ArrayList<>();
}