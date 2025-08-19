package com.caihuan.photo_app_backend.repository;

import com.caihuan.photo_app_backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @Author nanako
 * @Date 2025/8/13
 * @Description 评论仓库
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 根据照片ID查找该照片下的所有评论。
    List<Comment> findByPhotoId(Long photoId);
}
