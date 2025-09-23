package com.caihuan.photo_app_backend.controller;

import com.caihuan.photo_app_backend.payload.dto.PhotoArchiveDto;
import com.caihuan.photo_app_backend.services.BatchProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author nanako
 * @Date 2025/8/21
 * @Description 负责处理来自客户端的批量处理请求
 */
@RestController
@RequestMapping("/api/batch")
@CrossOrigin(origins = "*", maxAge = 3600)
public class BatchProcessController {

    @Autowired
    private BatchProcessService batchProcessService;

    /**
     * 接收并处理前端经过本地AI初筛后，批量上传的数字档案
     * @param albumId 目标相册ID
     * @param archives 包含缩略图和其他元数据的数字档案列表
     * @return 操作结果
     */
    @PostMapping("/sync/{albumId}")
    public ResponseEntity<?> syncArchives(@PathVariable Long albumId, @RequestBody List<PhotoArchiveDto> archives) {
        try {
            batchProcessService.processAndSaveArchives(albumId, archives);
            return ResponseEntity.ok("同步成功 " + archives.size() + " 个文件");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("处理过程发生错误, 详情：" + e.getMessage());
        }
    }
}

