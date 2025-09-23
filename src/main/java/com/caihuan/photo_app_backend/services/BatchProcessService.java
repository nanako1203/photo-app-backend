package com.caihuan.photo_app_backend.services;

import com.caihuan.photo_app_backend.entity.Album;
import com.caihuan.photo_app_backend.entity.Photo;
import com.caihuan.photo_app_backend.payload.dto.PhotoArchiveDto;
import com.caihuan.photo_app_backend.repository.AlbumRepository;
import com.caihuan.photo_app_backend.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchProcessService {

    private static final Logger logger = LoggerFactory.getLogger(BatchProcessService.class);

    private final S3Service s3Service;
    private final PhotoRepository photoRepository;
    private final AlbumRepository albumRepository;

    @Transactional
    public void processAndSaveArchives(Long albumId, List<PhotoArchiveDto> archives) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("处理批量上传失败：未找到 ID 为 " + albumId + " 的相册"));

        logger.info("开始同步相册 '{}' 的 {} 个数字档案...", album.getName(), archives.size());

        for (PhotoArchiveDto archive : archives) {
            try {
                // 1. 解码前端传来的两张图片
                byte[] thumbnailBytes = Base64.getDecoder().decode(archive.getThumbnailBase64());
                byte[] previewBytes = Base64.getDecoder().decode(archive.getPreviewBase64()); // 确保DTO和前端都已修改

                // 2. 上传缩略图，获取完整的URL
                String thumbnailUrl = s3Service.uploadThumbnail(thumbnailBytes, archive.getOriginalFileName());
                logger.info("文件 {} 的缩略图上传成功，URL是: {}", archive.getOriginalFileName(), thumbnailUrl);

                // 3. 上传分析图，获取 S3 Object Key
                String analysisImageKey = s3Service.uploadAnalysisImageAndReturnKey(previewBytes, archive.getOriginalFileName());
                logger.info("文件 {} 的可分析图片上传成功，Key是: {}", archive.getOriginalFileName(), analysisImageKey);

                // 4. 创建Photo对象并分别设置URL和Key
                Photo photo = new Photo();
                photo.setAlbum(album);
                photo.setOriginalFileName(archive.getOriginalFileName());
                photo.setLocalCategory(archive.getLocalCategory());

                // 【UI使用】设置完整的缩略图URL
                photo.setStorageUrl(thumbnailUrl);

                // 【AI分析使用】设置干净的S3 Object Key
                photo.setAnalysisImageKey(analysisImageKey); // 确保Photo实体已添加此字段

                photoRepository.save(photo);
                logger.info("已将照片 {} 的记录成功保存到数据库。", archive.getOriginalFileName());

            } catch (Exception e) {
                logger.error("处理档案 {} 时发生错误: {}", archive.getOriginalFileName(), e.getMessage(), e);
            }
        }
        logger.info("相册 '{}' 的数字档案同步完成。", album.getName());
    }
}