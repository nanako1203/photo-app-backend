package com.caihuan.photo_app_backend.controller;

import com.caihuan.photo_app_backend.entity.Album;
import com.caihuan.photo_app_backend.entity.Comment;
import com.caihuan.photo_app_backend.entity.Photo;
import com.caihuan.photo_app_backend.payload.request.CommentRequest;
import com.caihuan.photo_app_backend.payload.response.SharedAlbumResponse;
import com.caihuan.photo_app_backend.repository.AlbumRepository;
import com.caihuan.photo_app_backend.repository.CommentRepository;
import com.caihuan.photo_app_backend.repository.PhotoRepository;
import com.caihuan.photo_app_backend.services.S3Service; // 【修复】导入 S3Service
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor // 使用构造函数注入
public class PublicController {

    // 【修复】将 @Autowired 字段注入改为 final 字段，由 @RequiredArgsConstructor 负责注入
    private final AlbumRepository albumRepository;
    private final PhotoRepository photoRepository;
    private final CommentRepository commentRepository;
    private final S3Service s3Service; // 【修复】注入 S3Service

    // 1. 获取分享的相册及其照片
    @GetMapping("/album/{shareToken}")
    public ResponseEntity<?> getAlbumByShareToken(@PathVariable String shareToken) {
        Optional<Album> albumData = albumRepository.findByShareToken(shareToken);
        if (albumData.isPresent()) {
            Album album = albumData.get();
            List<Photo> photos = photoRepository.findByAlbumId(album.getId());

            // 【修复 Bug 1】为分享页面的照片生成预签名 URL，否则客户无法看到图片
            photos.forEach(photo -> {
                String originalUrl = photo.getStorageUrl();
                if (originalUrl != null && !originalUrl.isEmpty()) {
                    String objectKey = s3Service.getObjectKeyFromUrl(originalUrl);
                    if (objectKey != null) {
                        String presignedUrl = s3Service.generatePresignedUrl(objectKey);
                        photo.setStorageUrl(presignedUrl);
                    }
                }
            });

            return ResponseEntity.ok(new SharedAlbumResponse(album, photos));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 2. 客户点赞或取消点赞照片
    @PostMapping("/like/{shareToken}/{photoId}")
    public ResponseEntity<?> toggleLikePhoto(@PathVariable String shareToken, @PathVariable Long photoId) {
        if (!isPhotoInSharedAlbum(shareToken, photoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权访问此照片");
        }

        Optional<Photo> photoData = photoRepository.findById(photoId);
        if (photoData.isPresent()) {
            Photo photo = photoData.get();
            photo.setLikedByClient(!photo.isLikedByClient());
            Photo updatedPhoto = photoRepository.save(photo);
            return ResponseEntity.ok(updatedPhoto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 3. 获取一张照片下的所有评论
    @GetMapping("/comments/{shareToken}/{photoId}")
    public ResponseEntity<?> getCommentsForPhoto(@PathVariable String shareToken, @PathVariable Long photoId) {
        if (!isPhotoInSharedAlbum(shareToken, photoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权访问此照片的评论");
        }
        List<Comment> comments = commentRepository.findByPhotoId(photoId);
        return ResponseEntity.ok(comments);
    }

    // 4. 为一张照片添加评论
    @PostMapping("/comments/{shareToken}/{photoId}")
    public ResponseEntity<?> addCommentToPhoto(@PathVariable String shareToken, @PathVariable Long photoId, @Valid @RequestBody CommentRequest commentRequest) {
        if (!isPhotoInSharedAlbum(shareToken, photoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权评论此照片");
        }

        // 1. 首先，我们需要从数据库中获取完整的 Photo 对象
        Optional<Photo> photoOpt = photoRepository.findById(photoId);
        if (photoOpt.isEmpty()) {
            // 如果照片不存在，就不能评论，返回一个“未找到”的错误
            return ResponseEntity.notFound().build();
        }

        // 2. 创建新的 Comment 对象
        Comment comment = new Comment();

        // 3. 【核心修改】直接设置完整的 Photo 对象，而不是 photoId
        comment.setPhoto(photoOpt.get());
        // commenterName 在您的 CommentRequest DTO 中可能叫 commenterName 或 contentName
        comment.setCommenterName(commentRequest.getCommenterName());
        comment.setContent(commentRequest.getContent());
        Comment savedComment = commentRepository.save(comment);
        return ResponseEntity.ok(savedComment);
    }


    /**
     * 这是一个核心的安全检查辅助方法。
     * 它验证给定的photoId是否真的属于由shareToken指定的相册。
     */
    private boolean isPhotoInSharedAlbum(String shareToken, Long photoId) {
        Optional<Album> albumOpt = albumRepository.findByShareToken(shareToken);
        if (albumOpt.isEmpty()) {
            return false;
        }
        Optional<Photo> photoOpt = photoRepository.findById(photoId);
        if (photoOpt.isEmpty()) {
            return false;
        }

        Album photoAlbum = photoOpt.get().getAlbum();
        if (photoAlbum == null) {
            return false;
        }

        // 【修复 Bug 2】比较照片所属相册的ID和令牌对应相册的ID
        return photoAlbum.getId().equals(albumOpt.get().getId());
    }
}