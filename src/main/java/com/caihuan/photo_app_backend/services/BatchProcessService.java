package com.caihuan.photo_app_backend.services;

import com.caihuan.photo_app_backend.entity.Photo;
import com.caihuan.photo_app_backend.payload.dto.PhotoArchiveDto;
import com.caihuan.photo_app_backend.repository.PhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Base64;
import java.util.List;

/**
 * @Author nanako
 * @Date 2025/8/21
 * @Description 批量处理服务
 */

@Service
public class BatchProcessService {
    @Autowired
    private S3Service s3Service;

    private PhotoRepository photoRepository;

    @Transactional(rollbackFor = Exception.class)
    public void processAndSaveArchives(Long albumId, List<PhotoArchiveDto> archives) throws Exception {

        for (PhotoArchiveDto archive : archives) {
            //s缩略图字符串解码成字节数组
            byte[] thumbnailBytes = Base64.getDecoder().decode(archive.getThumbnailBase64());

            //上传云端得到链接
            String thumbnailUrl = s3Service.uploadFile(thumbnailBytes, archive.getOriginalFileName());

            //创建一个新的photo对象
            Photo photo = new Photo();
            photo.setAlbumId(albumId);
            photo.setStorageUrl(thumbnailUrl);
            photo.setLocalCategory(archive.getLocalCategory());
            photo.setOriginalFileName(archive.getOriginalFileName());
            photoRepository.save(photo);

        }

    }

}
