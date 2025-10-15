package com.caihuan.photo_app_backend.services;

import com.caihuan.photo_app_backend.entity.Album;
import com.caihuan.photo_app_backend.entity.Photo;
import com.caihuan.photo_app_backend.exception.ResourceNotFoundException;
import com.caihuan.photo_app_backend.repository.AlbumRepository;
import com.caihuan.photo_app_backend.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final PhotoRepository photoRepository;
    private final S3Service s3Service;

    @Transactional // 保证整个方法是一个原子操作，失败则回滚数据库
    public void deleteAlbumAndAssociatedPhotos(Long albumId, Long userId) {
        // 1. 查找相册，并验证所有权
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID为 " + albumId + " 的相册"));

        if (!album.getUser().getId().equals(userId)) {
            // 安全检查，防止用户删除不属于自己的相册
            throw new SecurityException("无权删除此相册");
        }

        // 2. 从数据库中找出此相册下的所有照片
        List<Photo> photosToDelete = photoRepository.findByAlbumId(albumId);

        // 3. 收集所有需要从S3删除的文件的Key
        if (photosToDelete != null && !photosToDelete.isEmpty()) {
            List<String> s3KeysToDelete = new ArrayList<>();
            for (Photo photo : photosToDelete) {
                // 使用你 S3Service 中已有的 getObjectKeyFromUrl 方法
                if (photo.getStorageUrl() != null && !photo.getStorageUrl().isEmpty()) {
                    s3KeysToDelete.add(s3Service.getObjectKeyFromUrl(photo.getStorageUrl()));
                }
                if (photo.getAnalysisImageKey() != null && !photo.getAnalysisImageKey().isEmpty()) {
                    s3KeysToDelete.add(photo.getAnalysisImageKey());
                }
            }
            
            List<String> validKeys = s3KeysToDelete.stream()
                .filter(key -> key != null && !key.isEmpty())
                .collect(Collectors.toList());

            // 4. 命令S3Service执行批量删除
            if (!validKeys.isEmpty()) {
                s3Service.deleteObjects(validKeys);
            }
        }

        // 5. 从数据库中删除相册记录
        // (前提：在Album实体中设置了CascadeType.ALL, orphanRemoval=true)
        albumRepository.delete(album);
    }
}