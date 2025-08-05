package com.caihuan.photo_app_backend.repository;

import com.caihuan.photo_app_backend.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @Author nanako
 * @Date 2025/8/1
 * @Description 相册仓库
 */
public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findByUserId(Long userId);
}
