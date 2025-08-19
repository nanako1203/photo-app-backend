package com.caihuan.photo_app_backend.repository;

import com.caihuan.photo_app_backend.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @Author nanako
 * @Date 2025/8/1
 * @Description 相册仓库
 */
public interface AlbumRepository extends JpaRepository<Album, Long> {

    //根据用户id检查该用户的所有相册 返回相册列表
    List<Album> findByUserId(Long userId);

    //根据token查找相册，用于客户通过分享链接访问。
    Optional<Album> findByShareToken(String shareToken);
}
