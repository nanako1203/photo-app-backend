package com.caihuan.photo_app_backend.controller;

import com.caihuan.photo_app_backend.entity.Photo;
import com.caihuan.photo_app_backend.payload.dto.PhotoArchiveDto;
import com.caihuan.photo_app_backend.repository.PhotoRepository;
import com.caihuan.photo_app_backend.services.BatchProcessService;
import com.caihuan.photo_app_backend.services.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;

/**
 * @Author nanako
 * @Date 2025/8/21
 * @Description 负责处理来自客户端的批量处理请求
 */

@RestController
@RequestMapping("api/batch")
@CrossOrigin(origins = "*", maxAge = 3600)
public class BatchProcessController {

    @Autowired
    private BatchProcessService batchProcessService;

    //接收并处理批量上传的数字档案
    public ResponseEntity<?> sysArchives(@PathVariable Long albumId, @RequestBody List<PhotoArchiveDto> archives) {

            try {
                batchProcessService.processAndSaveArchives(albumId, archives);
                return ResponseEntity.ok("同步成功" + archives.size() + "个文件");

            }catch (Exception e){
                e.printStackTrace();
                return ResponseEntity.badRequest().body("处理过程发生错误,详情：" + e.getMessage());
            }
    }
}