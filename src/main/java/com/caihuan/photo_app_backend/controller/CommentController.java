package com.caihuan.photo_app_backend.controller;

import com.caihuan.photo_app_backend.entity.Comment;
import com.caihuan.photo_app_backend.payload.request.CommentRequest;
import com.caihuan.photo_app_backend.repository.CommentRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author nanako
 * @Date 2025/8/21
 * @Description 评论请求处理
 */

@RestController
@RequestMapping("api/photos/{photoId}/comments")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;

    //获取一张照片下的所有评论
    @PostMapping
    public ResponseEntity<Comment> addCommentToPhoto  (@PathVariable Long photoId, @Valid CommentRequest commentRequest) {
        Comment comment = new Comment();
        comment.setPhotoId(photoId);
        comment.setContentName(commentRequest.getCommenterName());
        comment.setContent(commentRequest.getContent());
        Comment savedComment = commentRepository.save(comment);
        return ResponseEntity.ok(savedComment);
    }

    // [新增] 获取一张照片下的所有评论
    @GetMapping // <--- 添加这个
    public ResponseEntity<List<Comment>> getCommentsForPhoto(@PathVariable Long photoId) {
        List<Comment> comments = commentRepository.findByPhotoId(photoId);
        return ResponseEntity.ok(comments);
    }

}
