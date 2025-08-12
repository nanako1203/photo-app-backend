package com.caihuan.photo_app_backend.controller;

import com.caihuan.photo_app_backend.entity.Photo;
import com.caihuan.photo_app_backend.repository.PhotoRepository;
import com.caihuan.photo_app_backend.security.services.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    //@PathVariable捕获「albumId」的值并赋值给albumId @RequestParam接收从前端上传的文件，MultipartFile封装文件数据的标准类型
    @PostMapping("/upload/{albumId")
    public ResponseEntity<?> uploadPhoto(@PathVariable Long albumId, @RequestParam("file") MultipartFile file) {
        try {
            //上传到aws并返回公开访问地址
            String fileUrl = s3Service.upLoadFile(file);

            //将url获取的内容设置到新的photo对象
            Photo photo = new Photo();
            photo.setAlbumId(albumId);
            photo.setStorageUrl(fileUrl);
            photoRepository.save(photo);

            //保存到数据库的photo表中
            return ResponseEntity.ok(photo);


        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件上传失败" + e.getMessage());
        }
    }
}
