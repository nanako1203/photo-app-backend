package com.caihuan.photo_app_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    //照片所属相册id
    private Long albumId;

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

    //存储ai标签 @Lob JPA会自动将这个字段映射成数据库里的 TEXT 或 CLOB 类型
    @Lob
    private String aiLabels;

    //存储识别出的文字
    @Lob
    private String aiDetectedText;

    //存储主要颜色
    private String aiDominantColors;

    //照片在摄影师本地电脑所属文件夹分类标签
    private String localCategory;


}
