package com.caihuan.photo_app_backend.controller;

import com.caihuan.photo_app_backend.entity.Photo;
import com.caihuan.photo_app_backend.payload.request.CloudAnalysisRequest;
import com.caihuan.photo_app_backend.payload.response.ImageAnalysisResponse;
import com.caihuan.photo_app_backend.repository.PhotoRepository;
import com.caihuan.photo_app_backend.services.ImageAnalysisService;
import com.caihuan.photo_app_backend.services.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @Author nanako
 * @Date 2025/8/12
 * @Description 照片控制器, 负责处理与单张照片相关的精细化操作
 */
@RestController
@RequestMapping("/api/photos")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor // 使用 Lombok 进行构造函数注入
public class PhotoController {

    // 【优化】使用 final 字段和构造函数注入，替代 @Autowired
    private final S3Service s3Service;
    private final PhotoRepository photoRepository;
    private final ImageAnalysisService imageAnalysisService;
    private static final Logger logger = LoggerFactory.getLogger(PhotoController.class);

    /**
     * 获取指定相册下的所有照片信息
     * (此方法保持不变)
     */
    @GetMapping("/album/{albumId}")
    public ResponseEntity<List<Photo>> getPhotosByAlbumId(@PathVariable Long albumId) {
        List<Photo> photos = photoRepository.findByAlbumId(albumId);
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
        return ResponseEntity.ok(photos);
    }

    /**
     * 获取指定相册下所有被客户标记为“喜欢”的照片
     * (此方法保持不变)
     */
    @GetMapping("/album/{albumId}/liked")
    public ResponseEntity<List<Photo>> getLikedPhotosByAlbum(@PathVariable Long albumId) {
        List<Photo> likedPhotos = photoRepository.findByAlbumIdAndIsLikedByClientTrue(albumId);
        likedPhotos.forEach(photo -> {
            String originalUrl = photo.getStorageUrl();
            if (originalUrl != null && !originalUrl.isEmpty()) {
                String objectKey = s3Service.getObjectKeyFromUrl(originalUrl);
                if (objectKey != null) {
                    String presignedUrl = s3Service.generatePresignedUrl(objectKey);
                    photo.setStorageUrl(presignedUrl);
                }
            }
        });
        return ResponseEntity.ok(likedPhotos);
    }

    /**
     * 【此方法已按最终方案修改】
     * 接收前端按需上传的单个原始图片文件（Base64），进行云端AI深度分析
     * 新流程: Base64 -> 上传S3 -> 从S3分析 -> 删除S3临时文件 -> 返回结果
     */
    @PostMapping("/analyze-cloud")
    public ResponseEntity<?> analyzePhotoWithCloudAI(@Valid @RequestBody CloudAnalysisRequest request) {
        String tempFileUrl = null;
        String objectKey = null;
        try {
            // 1. 解码 Base64 字符串
            byte[] imageBytes = Base64.getDecoder().decode(request.getImageBase64());

            // 2. 将解码后的图片字节上传到 S3，以获取一个 objectKey
            // 我们为这个临时文件生成一个唯一的名字
            String tempFileName = "temp-analysis-" + UUID.randomUUID().toString() + ".jpg";
            tempFileUrl = s3Service.uploadThumbnail(imageBytes, tempFileName); // 复用 uploadThumbnail 方法即可
            objectKey = s3Service.getObjectKeyFromUrl(tempFileUrl);

            if (objectKey == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("无法上传临时文件到S3进行分析");
            }

            // 3. 【核心修复】调用新的、基于S3的分析服务
            ImageAnalysisResponse analysisResult = imageAnalysisService.analyzeImageFromS3(objectKey);

            // 4. 分析成功，返回结果
            return ResponseEntity.ok(analysisResult);

        } catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body("无效的Base64字符串：" + e.getMessage());
        } catch (IOException e) {
            logger.error("AI分析时发生IO错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("AI分析时发生错误: " + e.getMessage());
        } finally {
            // 5. 【重要】无论成功与否，都尝试删除在S3上创建的临时文件
            if (tempFileUrl != null) {
                try {
                    s3Service.deleteFile(tempFileUrl);
                    logger.info("已成功删除S3临时分析文件: {}", objectKey);
                } catch (Exception e) {
                    // 如果删除失败，只记录日志，不影响给前端的返回结果
                    logger.error("删除S3临时分析文件失败: {}", objectKey, e);
                }
            }
        }
    }

    /**
     * 删除一张照片
     * (此方法保持不变)
     */
    @DeleteMapping("/{photoId}")
    public ResponseEntity<String> deletePhoto(@PathVariable Long photoId) {
        Optional<Photo> photoData = photoRepository.findById(photoId);
        if (photoData.isPresent()) {
            Photo photo = photoData.get();
            try {
                if (photo.getStorageUrl() != null && !photo.getStorageUrl().isEmpty()) {
                    s3Service.deleteFile(photo.getStorageUrl());
                }
                if (photo.getFinalStorageUrl() != null && !photo.getFinalStorageUrl().isEmpty()) {
                    s3Service.deleteFile(photo.getFinalStorageUrl());
                }
                photoRepository.deleteById(photoId);
                return ResponseEntity.ok().body("照片删除成功");
            } catch (Exception e) {
                logger.error("删除照片 {} 失败", photoId, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除云端文件时出错");
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
