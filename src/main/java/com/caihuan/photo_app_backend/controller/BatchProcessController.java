package com.caihuan.photo_app_backend.controller;

import com.caihuan.photo_app_backend.entity.Photo;
import com.caihuan.photo_app_backend.payload.dto.PhotoArchiveDto;
import com.caihuan.photo_app_backend.services.BatchProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author nanako
 * @Date 2025/8/21
 * @Description 负责处理来自客户端的批量处理请求 (V2 - 支持双上传模式)
 */
@RestController
@RequestMapping("/api/batch")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor // 使用构造函数注入，更现代的写法
public class BatchProcessController {

    private final BatchProcessService batchProcessService;

    /**
     * 接收并处理前端经过本地AI初筛后，批量上传的数字档案。
     * 这个接口现在会返回一个包含已保存照片（带有ID）的列表。
     * @param albumId 目标相册ID
     * @param archives 包含元数据的数字档案列表
     * @return 包含新创建的照片实体的列表
     */
    @PostMapping("/sync/{albumId}")
    @PreAuthorize("hasRole('USER')") // 【重要】添加安全注解，确保只有登录用户能调用
    public ResponseEntity<List<Photo>> syncArchives(
            @PathVariable Long albumId,
            @RequestBody List<PhotoArchiveDto> archives) {

        List<Photo> savedPhotos = batchProcessService.processAndSaveArchives(albumId, archives);

        // 【核心修改】将保存后的照片列表（包含数据库ID）返回给前端
        return ResponseEntity.ok(savedPhotos);
    }
}

