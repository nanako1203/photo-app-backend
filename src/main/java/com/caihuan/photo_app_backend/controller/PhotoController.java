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

    // --- 这里是修改过的方法 ---
    @GetMapping("/album/{albumId}")
    public ResponseEntity<?> getPhotosByAlbumId(@PathVariable Long albumId) {
        try {
            // 这行是你的原始代码
            List<Photo> photos = photoRepository.findByAlbumId(albumId);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            // 如果上面的代码出现任何异常，我们会在这里捕获它
            System.out.println("!!!!!!!!!!!!!! 捕获到真正的异常 !!!!!!!!!!!!!!");
            e.printStackTrace(); // 在控制台打印出完整的、真正的错误堆栈信息
            // 返回一个 500 错误，并附带真实的错误消息
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取照片列表时发生内部错误: " + e.getMessage());
        }
    }
    // --- 修改结束 ---


    //获取指定相册所有被客户点赞的照片
    @GetMapping("album/{albumId}/liked")
    public ResponseEntity<List<Photo>> getLikedPhotosByAlbum(@PathVariable Long albumId) {
        List<Photo> likedPhotos = photoRepository.findByAlbumIdAndIsLikedByClientTrue(albumId);
        return ResponseEntity.ok(likedPhotos);
    }

    //删除一张照片
    @DeleteMapping("/{photoId}")
    public ResponseEntity<String> deletePhoto(@PathVariable Long photoId) {
        Optional<Photo> photoData = photoRepository.findById(photoId);
        if (photoData.isPresent()) {
            Photo photo = photoData.get();
            try {
                s3Service.deleteFile(photo.getStorageUrl());
                if (photo.getStorageUrl() != null) {
                    s3Service.deleteFile(photo.getFinalStorageUrl());
                }
                photoRepository.deleteById(photoId);
                return ResponseEntity.ok().body("照片删除成功");
            }catch (Exception e) {

                e.printStackTrace();
                return ResponseEntity.status(500).body("照片删除失败");
            }

        }else {
            return ResponseEntity.notFound().build();
        }
    }

    //web端直接上传
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
        return ResponseEntity.ok(results);
    }
}