package com.caihuan.photo_app_backend.services;

import com.caihuan.photo_app_backend.entity.Album;
import com.caihuan.photo_app_backend.entity.Photo;
import com.caihuan.photo_app_backend.exception.ResourceNotFoundException;
import com.caihuan.photo_app_backend.payload.dto.PhotoArchiveDto;
import com.caihuan.photo_app_backend.repository.AlbumRepository;
import com.caihuan.photo_app_backend.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public List<Photo> processAndSaveArchives(Long albumId, List<PhotoArchiveDto> archives) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("处理批量上传失败：未找到 ID 为 " + albumId + " 的相册"));

        logger.info("开始同步相册 '{}' 的 {} 个数字档案...", album.getName(), archives.size());

        List<Photo> savedPhotos = new ArrayList<>();

        for (PhotoArchiveDto archive : archives) {
            try {
                Photo photo = new Photo();
                photo.setAlbum(album);
                photo.setOriginalFileName(archive.getOriginalFileName());
                photo.setLocalCategory(archive.getLocalCategory());

                // 【核心修改】只有在 Base64 数据存在时才处理和上传图片
                if (archive.getThumbnailBase64() != null && !archive.getThumbnailBase64().isEmpty()) {
                    byte[] thumbnailBytes = Base64.getDecoder().decode(archive.getThumbnailBase64());
                    String thumbnailUrl = s3Service.uploadThumbnail(thumbnailBytes, archive.getOriginalFileName());
                    photo.setStorageUrl(thumbnailUrl); // UI 使用的缩略图
                }

                if (archive.getPreviewBase64() != null && !archive.getPreviewBase64().isEmpty()) {
                    byte[] previewBytes = Base64.getDecoder().decode(archive.getPreviewBase64());
                    String analysisImageKey = s3Service.uploadAnalysisImageAndReturnKey(previewBytes, archive.getOriginalFileName());
                    photo.setAnalysisImageKey(analysisImageKey); // AI 分析使用的预览图
                }

                Photo savedPhoto = photoRepository.save(photo);
                savedPhotos.add(savedPhoto); // 将保存后的对象（包含ID）加入列表
                logger.info("已将照片 {} 的记录成功保存到数据库，ID为 {}", archive.getOriginalFileName(), savedPhoto.getId());

            } catch (Exception e) {
                logger.error("处理档案 {} 时发生错误: {}", archive.getOriginalFileName(), e.getMessage(), e);
                // 选择跳过这个文件，继续处理下一个
            }
        }
        logger.info("相册 '{}' 的数字档案同步完成。", album.getName());
        return savedPhotos; // 返回包含新ID的照片列表
    }
}

