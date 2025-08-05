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
    List<Photo> findByAlbumId(long albumId);
}
