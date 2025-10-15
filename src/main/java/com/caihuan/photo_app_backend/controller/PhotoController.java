package com.caihuan.photo_app_backend.controller;

import com.caihuan.photo_app_backend.entity.Photo;
import com.caihuan.photo_app_backend.exception.ResourceNotFoundException;
import com.caihuan.photo_app_backend.payload.request.CloudAnalysisRequest;
import com.caihuan.photo_app_backend.payload.response.ImageAnalysisResponse;
import com.caihuan.photo_app_backend.payload.response.MessageResponse;
import com.caihuan.photo_app_backend.repository.PhotoRepository;
import com.caihuan.photo_app_backend.services.ImageAnalysisService;
import com.caihuan.photo_app_backend.services.S3Service;
import com.caihuan.photo_app_backend.services.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/photos")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class PhotoController {

    private final S3Service s3Service;
    private final PhotoRepository photoRepository;
    private final ImageAnalysisService imageAnalysisService;
    private static final Logger logger = LoggerFactory.getLogger(PhotoController.class);

    @GetMapping("/album/{albumId}")
    public ResponseEntity<List<Photo>> getPhotosByAlbumId(@PathVariable Long albumId) {
        List<Photo> photos = photoRepository.findByAlbumId(albumId);
        photos.forEach(this::generatePresignedUrlsForPhoto);
        return ResponseEntity.ok(photos);
    }

    @GetMapping("/album/{albumId}/liked")
    public ResponseEntity<List<Photo>> getLikedPhotosByAlbum(@PathVariable Long albumId) {
        List<Photo> likedPhotos = photoRepository.findByAlbumIdAndIsLikedByClientTrue(albumId);
        likedPhotos.forEach(this::generatePresignedUrlsForPhoto);
        return ResponseEntity.ok(likedPhotos);
    }

    @PostMapping("/analyze-cloud")
    public ResponseEntity<?> analyzePhotoWithCloudAI(@Valid @RequestBody CloudAnalysisRequest request) {
        String tempFileUrl = null;
        String objectKey = null;
        try {
            byte[] imageBytes = Base64.getDecoder().decode(request.getImageBase64());
            String tempFileName = "temp-analysis-" + UUID.randomUUID().toString() + ".jpg";
            tempFileUrl = s3Service.uploadThumbnail(imageBytes, tempFileName);
            objectKey = s3Service.getObjectKeyFromUrl(tempFileUrl);

            if (objectKey == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("无法上传临时文件到S3进行分析"));
            }

            ImageAnalysisResponse analysisResult = imageAnalysisService.analyzeImageFromS3(objectKey);
            return ResponseEntity.ok(analysisResult);

        } catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(new MessageResponse("无效的Base64字符串：" + e.getMessage()));
        } catch (IOException e) {
            logger.error("AI分析时发生IO错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("AI分析时发生错误: " + e.getMessage()));
        } finally {
            if (tempFileUrl != null) {
                try {
                    s3Service.deleteFile(tempFileUrl);
                    logger.info("已成功删除S3临时分析文件: {}", objectKey);
                } catch (Exception e) {
                    logger.error("删除S3临时分析文件失败: {}", objectKey, e);
                }
            }
        }
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<?> deletePhoto(@PathVariable Long photoId) {
        Optional<Photo> photoData = photoRepository.findById(photoId);
        if (photoData.isPresent()) {
            Photo photo = photoData.get();
            try {
                // 使用批量删除逻辑，更健壮
                List<String> keysToDelete = new ArrayList<>();
                if (photo.getStorageUrl() != null && !photo.getStorageUrl().isEmpty()) {
                    keysToDelete.add(s3Service.getObjectKeyFromUrl(photo.getStorageUrl()));
                }
                if (photo.getFinalStorageUrl() != null && !photo.getFinalStorageUrl().isEmpty()) {
                    keysToDelete.add(s3Service.getObjectKeyFromUrl(photo.getFinalStorageUrl()));
                }
                if (photo.getAnalysisImageKey() != null && !photo.getAnalysisImageKey().isEmpty()) {
                    keysToDelete.add(photo.getAnalysisImageKey());
                }
                if (!keysToDelete.isEmpty()) {
                    s3Service.deleteObjects(keysToDelete);
                }

                photoRepository.deleteById(photoId);
                return ResponseEntity.ok().body(new MessageResponse("照片删除成功"));
            } catch (Exception e) {
                logger.error("删除照片 {} 失败", photoId, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("删除云端文件时出错"));
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/upload-final")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> uploadFinalPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();

        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID为 " + id + " 的照片"));

        if (!photo.getAlbum().getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse("无权修改此照片"));
        }

        try {
            List<String> keysToDelete = new ArrayList<>();
            if (photo.getFinalStorageUrl() != null && !photo.getFinalStorageUrl().isEmpty()) {
                keysToDelete.add(s3Service.getObjectKeyFromUrl(photo.getFinalStorageUrl()));
            }
            if (photo.getStorageUrl() != null && !photo.getStorageUrl().isEmpty()) {
                keysToDelete.add(s3Service.getObjectKeyFromUrl(photo.getStorageUrl()));
            }
            if (photo.getAnalysisImageKey() != null && !photo.getAnalysisImageKey().isEmpty()) {
                keysToDelete.add(photo.getAnalysisImageKey());
            }

            if (!keysToDelete.isEmpty()) {
                s3Service.deleteObjects(keysToDelete);
                logger.info("已为照片 {} 批量删除 {} 个旧文件。", id, keysToDelete.size());
            }

            String finalUrl = s3Service.uploadFinalVersion(file);

            photo.setFinalStorageUrl(finalUrl);
            photo.setStorageUrl(null);
            photo.setAnalysisImageKey(null);
            photo.setFinalized(true);
            photoRepository.save(photo);

            return ResponseEntity.ok(new MessageResponse("精修大图上传成功！旧的预览图已清理。"));

        } catch (Exception e) {
            logger.error("上传精修图失败，照片ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("上传失败：" + e.getMessage()));
        }
    }

    /**
     * 【核心修复】辅助方法，为单张照片的所有URL生成预签名链接
     */
    private void generatePresignedUrlsForPhoto(Photo photo) {
        // 1. 缩略图
        String thumbnailUrl = photo.getStorageUrl();
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            String objectKey = s3Service.getObjectKeyFromUrl(thumbnailUrl);
            if (objectKey != null) {
                photo.setStorageUrl(s3Service.generatePresignedUrl(objectKey));
            }
        }
        // 2. 高清最终图
        String finalUrl = photo.getFinalStorageUrl();
        if (finalUrl != null && !finalUrl.isEmpty()) {
            String objectKey = s3Service.getObjectKeyFromUrl(finalUrl);
            if (objectKey != null) {
                photo.setFinalStorageUrl(s3Service.generatePresignedUrl(objectKey));
            }
        }
        // 3. 中尺寸预览图
        String analysisKey = photo.getAnalysisImageKey();
        if (analysisKey != null && !analysisKey.isEmpty()) {
            photo.setPreviewUrl(s3Service.generatePresignedUrl(analysisKey));
        }
    }
}

