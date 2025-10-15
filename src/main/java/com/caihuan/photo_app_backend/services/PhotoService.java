package com.caihuan.photo_app_backend.services;

import com.caihuan.photo_app_backend.entity.Photo;
import com.caihuan.photo_app_backend.payload.response.ImageAnalysisResponse;
import com.caihuan.photo_app_backend.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final S3Service s3Service;
    private final ImageAnalysisService imageAnalysisService;

    private static final Logger logger = LoggerFactory.getLogger(PhotoService.class);

    @Async
    public void analyzePhotosInAlbum(Long albumId) {
        List<Photo> photosToAnalyze = photoRepository.findByAlbumIdAndCloudAnalyzedIsFalse(albumId);
        logger.info("后台任务开始：分析相册 {} 中的 {} 张照片...", albumId, photosToAnalyze.size());
        for (Photo photo : photosToAnalyze) {
            analyzeAndSaveSinglePhoto(photo);
        }
        logger.info("后台任务完成：相册 {} 的所有照片分析结束！", albumId);
    }

    private void analyzeAndSaveSinglePhoto(Photo photo) {
        String objectKey = photo.getAnalysisImageKey();

        if (objectKey == null || objectKey.isEmpty()) {
            logger.error("分析失败：照片 {} 没有可供分析的Image Key。", photo.getOriginalFileName());
            return;
        }

        try {
            logger.info("正在通过 S3 引用分析照片: {}", objectKey);
            ImageAnalysisResponse analysisResult = imageAnalysisService.analyzeImageFromS3(objectKey);

            // 【核心修改】将丰富的分析结果更新到照片对象中
            // 内容识别
            photo.setCategories(analysisResult.getCategories()); // <-- 使用新的字段
            photo.setAiLabels(analysisResult.getLabels());
//            photo.setAiDetectedText(analysisResult.getDetectedText()); // 你的Photo实体中没有这个字段，如果需要请添加
            // photo.setAiDominantColors(analysisResult.getDominantColors()); // 新的Service中已移除

            // 人脸分析
            photo.setFaceCount(analysisResult.getFaceCount());
            photo.setAllFacesSmiling(analysisResult.isAllFacesSmiling());
            photo.setAllEyesOpen(analysisResult.isAllEyesOpen());

            photo.setCloudAnalyzed(true); // 标记为已分析

            photoRepository.save(photo);
            logger.info("照片 {} 的云端AI分析成功并已保存。", photo.getOriginalFileName());

        } catch (Exception e) {
            logger.error("分析照片 {} (Key: {}) 失败: {}", photo.getOriginalFileName(), objectKey, e.getMessage());
        }
    }
}