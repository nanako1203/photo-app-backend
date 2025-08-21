package com.caihuan.photo_app_backend.controller;

import com.caihuan.photo_app_backend.entity.Photo;
import com.caihuan.photo_app_backend.payload.dto.ImageAnalysisResult;
import com.caihuan.photo_app_backend.payload.request.AiAnalysisRequest;
import com.caihuan.photo_app_backend.repository.PhotoRepository;
import com.caihuan.photo_app_backend.services.ImageAnalysisService;
import com.caihuan.photo_app_backend.services.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @Author nanako
 * @Date 2025/8/12
 * @Description 照片控制器
 */

@RestController
@RequestMapping("/api/photos")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PhotoController {

    @Autowired
    private S3Service s3Service;
    @Autowired
    private PhotoRepository photoRepository;
    @Autowired
    private ImageAnalysisService imageAnalysisService;

    //获取指定相册所有照片
    @GetMapping("/album/{albumId}")//{albumId}路径变量 用实际id替换
    public ResponseEntity<List<Photo>> getPhotosByAlbumId(@PathVariable Long albumId) {
        List<Photo> photos = photoRepository.findByAlbumId(albumId);
        return ResponseEntity.ok(photos);
    }

    //获取指定相册所有被客户点赞的照片
    @GetMapping("album/{albumId}/liked")
    public ResponseEntity<List<Photo>> getLikedPhotosByAlbum(@PathVariable Long albumId) {
        List<Photo> likedPhotos = photoRepository.findByAlbumIdAndIsLikedByClientTrue(albumId);
        return ResponseEntity.ok(likedPhotos);
    }

    //@PathVariable捕获「albumId」的值并赋值给albumId @RequestParam接收从前端上传的文件，MultipartFile封装文件数据的标准类型
    @PostMapping("/upload/{albumId}")
    public ResponseEntity<?> uploadPhoto(@PathVariable Long albumId, @RequestParam("file") MultipartFile file) {
        try {
            //上传到aws并返回公开访问地址
            String fileUrl = s3Service.upLoadFile(file);
            ImageAnalysisResult analysisResult = imageAnalysisService.analyzeImage(file);

            //将url获取的内容设置到新的photo对象
            Photo photo = new Photo();
            photo.setAlbumId(albumId);
            photo.setStorageUrl(fileUrl);
            photo.setSceneCategory(analysisResult.getSceneCategory());
            if (analysisResult.getLabels() != null) {
                photo.setAiLabels(String.join(",", analysisResult.getLabels()));
            }
            if (analysisResult.getDetectedText() != null) {
                photo.setAiDetectedText(analysisResult.getDetectedText());
            }
            if (analysisResult.getDominantColors() != null) {
                photo.setAiDominantColors(String.join(",", analysisResult.getDominantColors()));
            }

            photoRepository.save(photo);

            //保存到数据库的photo表中
            return ResponseEntity.ok(photo);


        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件上传失败" + e.getMessage());
        }
    }

    //web端请求云端ai精选
    @PostMapping("/analyze-selection")
    public ResponseEntity<?> analyzeSelectedPhotos(@RequestBody AiAnalysisRequest request) {
        List<ImageAnalysisResult> results = new ArrayList<>();
        for (Long photoId : request.getPhotoIds()) {
            Optional<Photo> photoOpt = photoRepository.findById(photoId);
            if (photoOpt.isPresent()) {
                try {
                    byte[] imageBytes = s3Service.downloadFile(photoOpt.get().getStorageUrl());
                    ImageAnalysisResult result = imageAnalysisService.analyzeImageBytes(imageBytes);
                    results.add(result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
