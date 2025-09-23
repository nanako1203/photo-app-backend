package com.caihuan.photo_app_backend.services;

import com.caihuan.photo_app_backend.entity.Photo;
import com.caihuan.photo_app_backend.payload.response.ImageAnalysisResponse;
import com.caihuan.photo_app_backend.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor // 使用 Lombok 进行构造函数注入
public class PhotoService {

    // 注入我们需要的“储藏室”和“供应商”
    private final PhotoRepository photoRepository;
    private final S3Service s3Service;
    private final ImageAnalysisService imageAnalysisService;

    // 【最佳实践】为本类添加一个专用的日志记录器
    private static final Logger logger = LoggerFactory.getLogger(PhotoService.class);

    /**
     * 【异步方法】在后台分析一个相册里的所有未分析过的照片
     * @param albumId 相册ID
     */
    @Async // 标记这个方法为异步执行，它会在一个独立的线程中运行
    public void analyzePhotosInAlbum(Long albumId) {
        // 1. 从数据库查找所有需要分析的照片
        List<Photo> photosToAnalyze = photoRepository.findByAlbumIdAndCloudAnalyzedIsFalse(albumId);

        logger.info("后台任务开始：分析相册 {} 中的 {} 张照片...", albumId, photosToAnalyze.size());

        // 2. 循环处理每一张照片
        for (Photo photo : photosToAnalyze) {
            // 3. 调用一个辅助方法来处理单张照片的逻辑
            // 【注意】我们将所有异常处理都封装在了辅助方法内部
            analyzeAndSaveSinglePhoto(photo);
        }
        logger.info("后台任务完成：相册 {} 的所有照片分析结束！", albumId);
    }

    /**
     * 【此方法已按最终方案修改】
     * 这是一个私有的辅助方法，封装了分析单张照片并保存的核心逻辑
     * @param photo 要处理的照片对象
     */
    private void analyzeAndSaveSinglePhoto(Photo photo) {
        // ====================== 【请确认这里的修改】 ======================
        // a. 我们不再从 photo.getStorageUrl() 中提取Key
        //    而是直接获取专用于分析的 photo.getAnalysisImageKey()
        String objectKey = photo.getAnalysisImageKey();
        // =============================================================

        if (objectKey == null || objectKey.isEmpty()) {
            logger.error("分析失败：照片 {} 没有可供分析的Image Key。可能是上传时出错。", photo.getOriginalFileName());
            return; // 如果Key不存在，则跳过这张照片
        }

        try {
            // 现在这里传递的 objectKey 应该是 "analysis_..." 开头的正确Key了
            logger.info("正在通过 S3 引用分析照片: {}", objectKey);
            ImageAnalysisResponse analysisResult = imageAnalysisService.analyzeImageFromS3(objectKey);


            // d. 将丰富的分析结果更新到照片对象中 (这部分逻辑保持不变)
            // 内容识别
            photo.setSceneCategory(analysisResult.getSceneCategory());
            photo.setAiLabels(analysisResult.getLabels());
            photo.setAiDetectedText(analysisResult.getDetectedText());
            photo.setAiDominantColors(analysisResult.getDominantColors());

            // 质量评估photo.setSharpness(analysisResult.getSharpness());photo.setBrightness(analysisResult.getBrightness());

            // 人脸分析
            photo.setFaceCount(analysisResult.getFaceCount());
            photo.setAllFacesSmiling(analysisResult.isAllFacesSmiling());
            photo.setAllEyesOpen(analysisResult.isAllEyesOpen());

            photo.setCloudAnalyzed(true); // 标记为已分析

            // e. 将更新后的照片信息保存回数据库
            photoRepository.save(photo);
            logger.info("照片 {} 的云端AI分析成功并已保存。", photo.getOriginalFileName());

        } catch (Exception e) {
            // 异常处理逻辑保持不变，记录错误并继续处理下一张照片
            logger.error("分析照片 {} (Key: {}) 失败: {}", photo.getOriginalFileName(), objectKey, e.getMessage());
        }
    }
}

