package com.caihuan.photo_app_backend.repository;

import com.caihuan.photo_app_backend.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @Author nanako
 * @Date 2025/8/1
 * @Description 照片仓库
 */
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    // 根据相册ID查找该相册下的所有照片
    List<Photo> findByAlbumId(long albumId);

    // 根据相册ID，查找该相册下所有被客户标记为“喜欢”的照片
    List<Photo> findByAlbumIdAndIsLikedByClientTrue(Long albumId);

    // 【新增】根据相册ID和分析状态查找照片
    List<Photo> findByAlbumIdAndCloudAnalyzedIsFalse(Long albumId);

}
