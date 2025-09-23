package com.caihuan.photo_app_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author nanako
 * @Date 2025/8/1
 * @Description photo实体类
 */

@Entity
@Table(name = "photos")
@Data
@NoArgsConstructor
public class Photo {

    //照片id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * @ManyToOne 表示“多对一”的关系（多张照片对应一个相册）
     * fetch = FetchType.LAZY 是性能优化，表示只有在真正需要访问Album对象时才从数据库加载它
     * @JoinColumn 指定了在数据库的 photos 表中，用来存储外键的列名是 album_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;
    // =======================================================

    //云存储网址
    private String storageUrl;

    //点赞
    private boolean isLikedByClient = false;

    // 标记摄影师是否已完成精修并上传了最终版本
    private boolean isFinalized = false;

    // 存储精修后高清大图在云存储上的URL
    private String finalStorageUrl;

    // 原始文件名 (例如: _DSC1234.JPG)，用于映射本地文件
    private String originalFileName;

    //场景类别
    private String sceneCategory;

    // 【修改】将 aiLabels 的类型从 String 改为 List<String>
    // @ElementCollection 会为这个列表自动创建一个关联表来存储
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> aiLabels;

    //存储识别出的文字
    @Lob
    private String aiDetectedText;

    // 【修改】将 aiDominantColors 也改为 List<String> 以匹配分析结果
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> aiDominantColors;

    //照片在摄影师本地电脑所属文件夹分类标签
    private String localCategory;

    // 【新增】添加 cloudAnalyzed 字段
    private boolean cloudAnalyzed = false;

    // =======================================================
    // 【新增】用于存储质量评估和人脸分析结果的字段
    // =======================================================

    /**
     * 图像质量 - 清晰度分数 (由 Rekognition 提供)
     */
    private Double sharpness;

    /**
     * 图像质量 - 亮度分数 (由 Rekognition 提供)
     */
    private Double brightness;

    /**
     * 人脸分析 - 在照片中检测到的人脸数量
     */
    private Integer faceCount;

    /**
     * 人脸分析 - 是否所有检测到的人脸都在微笑
     */
    private boolean allFacesSmiling;

    /**
     * 人脸分析 - 是否所有检测到的人脸都睁着眼睛
     */
    private boolean allEyesOpen;

    // 【新增字段】用于存储给AI分析的【可分析图片（如预览图）的S3 Key】
    @Column(name = "analysis_image_key") // 在数据库中对应的列名
    private String analysisImageKey;
}
